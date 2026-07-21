package com.sawhub.hub.comercial;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.comercial.dto.AvancarLeadRequest;
import com.sawhub.hub.comercial.dto.CriarLeadRequest;
import com.sawhub.hub.comercial.dto.FecharVendaRequest;
import com.sawhub.hub.comercial.dto.ParcelaVendaRequest;
import com.sawhub.hub.comercial.dto.VendaIngressoRequest;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H1.3 (solicitar acesso) + H13.2 (funil comercial). */
@Service
public class LeadService {

    // M26 — "todas as vendas e valores precisam ser mapeados no DRE" (confirmado pelo Marcos,
    // 19/07/2026): resolução ProdutoVenda→CategoriaFinanceira por nome, mesmo mecanismo já testado
    // em ContaCsvService/LancamentoCsvService pra resolver categoria de CSV. Vive aqui (comercial),
    // não em financeiro, porque comercial já depende de financeiro — o inverso criaria ciclo de
    // pacote (ver ROADMAP.md § "Blueprint (M26)"). Categorias seedadas via V40, garantidas em
    // qualquer ambiente (não só DemoDataSeeder).
    private static final Map<ProdutoVenda, String> CATEGORIA_POR_PRODUTO = Map.of(
            ProdutoVenda.MENTORIA_CONTINUA, "Mentoria Contínua",
            ProdutoVenda.MENTORIA_INDIVIDUAL, "Mentoria Individual",
            ProdutoVenda.CONSULTORIA, "Consultoria",
            ProdutoVenda.FORMULA_SAW, "Fórmula SAW",
            ProdutoVenda.FORMACAO_PROFISSIONAL, "Formação Profissional",
            ProdutoVenda.FICHA_TECNICA_LUCRATIVA, "Ficha Técnica Lucrativa",
            ProdutoVenda.PRODUTO_DIGITAL, "Produtos Digitais",
            ProdutoVenda.INGRESSO_EVENTO, "Eventos"
    );

    private final LeadRepository leadRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final AtividadeLogService atividadeLogService;
    private final ParcelaVendaRepository parcelaVendaRepository;
    private final VendaIngressoRepository vendaIngressoRepository;
    private final EventoRepository eventoRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    private final CategoriaFinanceiraRepository categoriaFinanceiraRepository;

    public LeadService(LeadRepository leadRepository, ColaboradorRepository colaboradorRepository,
                        AtividadeLogService atividadeLogService, ParcelaVendaRepository parcelaVendaRepository,
                        VendaIngressoRepository vendaIngressoRepository, EventoRepository eventoRepository,
                        LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
                        CategoriaFinanceiraRepository categoriaFinanceiraRepository) {
        this.leadRepository = leadRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.atividadeLogService = atividadeLogService;
        this.parcelaVendaRepository = parcelaVendaRepository;
        this.vendaIngressoRepository = vendaIngressoRepository;
        this.eventoRepository = eventoRepository;
        this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
        this.categoriaFinanceiraRepository = categoriaFinanceiraRepository;
    }

    @Transactional
    public Lead criar(CriarLeadRequest request) {
        Lead lead = new Lead(request.nome(), request.email(), request.telefone(), request.mensagem());
        return leadRepository.save(lead);
    }

    public List<Lead> listar(StatusLead status, UUID vendedorId) {
        return leadRepository.buscarComFiltro(status, vendedorId);
    }

    @Transactional
    public Lead avancar(UUID leadId, AvancarLeadRequest request) {
        Lead lead = leadRepository.buscarPorIdComVendedor(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado."));

        switch (request.novoStatus()) {
            case EM_CONTATO -> lead.moverParaEmContato(resolverVendedor(request.vendedorId()));
            // M25 — etapa opcional do funil real; quem não passa por aqui continua indo direto
            // de EM_CONTATO pra PROPOSTA (ver Lead.moverParaProposta).
            case DIAGNOSTICO -> lead.moverParaDiagnostico();
            case PROPOSTA -> lead.moverParaProposta();
            // M28 — o caminho legado de fechar um lead por aqui (Plano) foi removido junto com
            // Plano ("não existem planos, mas sim produtos"); fechar venda de verdade é só via
            // fecharVenda() (M25, formulário único). FECHADO continua existindo como StatusLead
            // (estado terminal real do funil), só não é mais alcançável por avancar().
            case FECHADO -> throw new IllegalArgumentException(
                    "Não é possível fechar um lead por aqui — use o endpoint de fechar venda.");
            case PERDIDO -> {
                if (request.motivoPerdido() == null || request.motivoPerdido().isBlank()) {
                    throw new IllegalArgumentException("Motivo é obrigatório para marcar o lead como Perdido.");
                }
                lead.perder(request.motivoPerdido());
                atividadeLogService.registrar("LEAD_PERDIDO", "Lead perdido: " + lead.getNome());
            }
            case SOLICITACAO -> throw new IllegalArgumentException("Não é possível mover um lead de volta para Solicitação.");
        }

        return leadRepository.save(lead);
    }

    /** M25 — "formulário único de venda": fecha o Lead com produto/origem/valor/forma de
     * pagamento, e distribui automaticamente o dado que hoje vive em planilhas separadas:
     * parcelamento vira lançamentos a receber, ingresso de evento vira credenciamento. M28 —
     * único caminho pra fechar um lead de verdade (o legado via avancar()/FECHADO foi removido
     * junto com Plano). M26 — valor pago no ato também passa a gerar um lançamento REALIZADO
     * (antes só ficava gravado no Lead, sem rastro nenhum no financeiro). */
    @Transactional
    public Lead fecharVenda(UUID leadId, FecharVendaRequest request) {
        Lead lead = leadRepository.buscarPorIdComVendedor(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead não encontrado."));

        // Achado B3 da revisão de segurança (M25), estendido pelo gap 7 (19/07/2026): sem esta
        // checagem dá pra registrar valor pago no ato (+ taxa de plataforma retida) maior que o
        // valor total da venda, corrompendo o financeiro silenciosamente. taxaPlataformaRetida
        // entra na mesma soma porque as duas juntas nunca podem passar do total — é a mesma
        // invariante de antes, só que agora com um terceiro conceito (ver Lead#fecharVenda).
        BigDecimal recebidoMaisTaxa = zeroSeNulo(request.valorPagoNoAto()).add(zeroSeNulo(request.taxaPlataformaRetida()));
        if (recebidoMaisTaxa.compareTo(request.valorTotalVenda()) > 0) {
            throw new IllegalArgumentException(
                    "Valor pago no ato mais taxa de plataforma retida não pode ultrapassar o valor total da venda.");
        }

        lead.fecharVenda(request.produtoVenda(), request.origemVenda(), request.valorTotalVenda(),
                request.valorPagoNoAto(), request.formaPagamento(), request.taxaPlataformaRetida());
        lead = leadRepository.save(lead);

        if (request.produtoVenda() == ProdutoVenda.INGRESSO_EVENTO) {
            criarVendasIngresso(lead, request);
        }
        if (request.valorPagoNoAto() != null && request.valorPagoNoAto().signum() > 0) {
            criarLancamentoValorPagoNoAto(lead, request.produtoVenda(), request.valorPagoNoAto());
        }
        if (request.parcelas() != null) {
            criarParcelas(lead, request.produtoVenda(), request.parcelas());
        }

        atividadeLogService.registrar("LEAD_VENDA_FECHADA", "Venda fechada: " + lead.getNome());
        return lead;
    }

    private void criarVendasIngresso(Lead lead, FecharVendaRequest request) {
        if (request.eventoId() == null) {
            throw new IllegalArgumentException("Evento é obrigatório para venda de ingresso.");
        }
        Evento evento = eventoRepository.findById(request.eventoId())
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));
        // Achado M2 da revisão de segurança (M25): venda de ingresso só faz sentido pra um evento
        // ainda em aberto — mesma janela de status já usada em EventoMentoradoService.inscrever().
        if (evento.getStatus() != StatusEvento.PROGRAMADO && evento.getStatus() != StatusEvento.AO_VIVO) {
            throw new IllegalArgumentException("Evento não está disponível para venda de ingresso (status " + evento.getStatus() + ").");
        }
        if (request.ingressos() == null || request.ingressos().isEmpty()) {
            throw new IllegalArgumentException("Pelo menos um ingresso é obrigatório para venda de ingresso.");
        }
        for (VendaIngressoRequest ingresso : request.ingressos()) {
            // Mesma invariante de capacidade do H7.2 (Evento.ocuparVaga()) — sem isso o funil
            // comercial vende ingresso além da capacidade do evento sem qualquer aviso.
            evento.ocuparVaga();
            vendaIngressoRepository.save(new VendaIngresso(lead, evento, ingresso.categoriaIngresso(),
                    ingresso.nomeCredenciado(), ingresso.setor(), ingresso.almoco(),
                    ingresso.nomeEmpresa(), ingresso.telefone(), ingresso.email()));
        }
        eventoRepository.save(evento);
    }

    /** M26 — dinheiro recebido no ato do fechamento já é receita realizada (diferente de parcela,
     * que é "a receber"), então nasce direto REALIZADO, sem `dataVencimento`. */
    private void criarLancamentoValorPagoNoAto(Lead lead, ProdutoVenda produtoVenda, BigDecimal valorPagoNoAto) {
        CategoriaFinanceira categoria = resolverCategoriaVenda(produtoVenda);
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria,
                "Pago no ato - " + lead.getNome(), valorPagoNoAto, LocalDate.now(), StatusLancamento.REALIZADO,
                null, null, null);
        lancamentoFinanceiroRepository.save(lancamento);
    }

    private void criarParcelas(Lead lead, ProdutoVenda produtoVenda, List<ParcelaVendaRequest> parcelas) {
        CategoriaFinanceira categoria = resolverCategoriaVenda(produtoVenda);
        for (ParcelaVendaRequest p : parcelas) {
            ParcelaVenda parcela = parcelaVendaRepository.save(new ParcelaVenda(lead, p.numero(), p.valor(), p.dataPrevista()));
            LancamentoFinanceiro lancamento = lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(
                    TipoLancamento.RECEITA, categoria, "Parcela " + p.numero() + " - " + lead.getNome(), p.valor(),
                    p.dataPrevista(), StatusLancamento.PREVISTO, null, null, p.dataPrevista()));
            parcela.vincularLancamento(lancamento);
            parcelaVendaRepository.save(parcela);
        }
    }

    private CategoriaFinanceira resolverCategoriaVenda(ProdutoVenda produtoVenda) {
        String nomeCategoria = CATEGORIA_POR_PRODUTO.get(produtoVenda);
        List<CategoriaFinanceira> candidatas = categoriaFinanceiraRepository.findByNomeIgnoreCase(nomeCategoria);
        if (candidatas.isEmpty()) {
            throw new IllegalStateException("Categoria financeira \"" + nomeCategoria + "\" não encontrada — "
                    + "esperada pré-cadastrada pela migration V40 pra vendas de " + produtoVenda + ".");
        }
        if (candidatas.size() > 1) {
            throw new IllegalStateException("Categoria financeira \"" + nomeCategoria + "\" é ambígua "
                    + "(existe mais de uma com esse nome).");
        }
        return candidatas.get(0);
    }

    private Colaborador resolverVendedor(UUID vendedorId) {
        if (vendedorId == null) {
            throw new IllegalArgumentException("Vendedor é obrigatório para mover o lead para Em contato.");
        }
        return colaboradorRepository.findById(vendedorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor não encontrado."));
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
