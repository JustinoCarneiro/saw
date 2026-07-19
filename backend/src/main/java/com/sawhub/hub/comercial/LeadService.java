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
import com.sawhub.hub.financeiro.ContaPagarReceber;
import com.sawhub.hub.financeiro.ContaPagarReceberRepository;
import com.sawhub.hub.financeiro.TipoConta;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H1.3 (solicitar acesso) + H13.2 (funil comercial). */
@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final AtividadeLogService atividadeLogService;
    private final ParcelaVendaRepository parcelaVendaRepository;
    private final VendaIngressoRepository vendaIngressoRepository;
    private final EventoRepository eventoRepository;
    private final ContaPagarReceberRepository contaPagarReceberRepository;

    public LeadService(LeadRepository leadRepository, ColaboradorRepository colaboradorRepository,
                        AtividadeLogService atividadeLogService, ParcelaVendaRepository parcelaVendaRepository,
                        VendaIngressoRepository vendaIngressoRepository, EventoRepository eventoRepository,
                        ContaPagarReceberRepository contaPagarReceberRepository) {
        this.leadRepository = leadRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.atividadeLogService = atividadeLogService;
        this.parcelaVendaRepository = parcelaVendaRepository;
        this.vendaIngressoRepository = vendaIngressoRepository;
        this.eventoRepository = eventoRepository;
        this.contaPagarReceberRepository = contaPagarReceberRepository;
    }

    @Transactional
    public Lead criar(CriarLeadRequest request) {
        Lead lead = new Lead(request.nome(), request.email(), request.telefone(), request.mensagem(),
                request.planoInteresse());
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
            // Achado L2 da revisão de segurança: sem exigir plano/motivo aqui, um FECHADO sem
            // plano quebra silenciosamente "vendas por plano" (H13.1), e um PERDIDO sem motivo
            // perde o dado que justifica a decisão pro time comercial revisar depois.
            case FECHADO -> {
                if (request.planoFechado() == null) {
                    throw new IllegalArgumentException("Plano fechado é obrigatório para mover o lead para Fechado.");
                }
                lead.fechar(request.planoFechado());
                atividadeLogService.registrar("LEAD_FECHADO", "Lead fechado: " + lead.getNome());
            }
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
     * pagamento (em vez de só o plano legado), e distribui automaticamente o dado que hoje vive
     * em planilhas separadas: parcelamento vira contas a receber, ingresso de evento vira
     * credenciamento. Endpoint dedicado, não substitui avancar()/FECHADO (que continua existindo
     * pra quem só precisa registrar o plano). */
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
        if (request.parcelas() != null) {
            criarParcelas(lead, request.parcelas());
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

    private void criarParcelas(Lead lead, List<ParcelaVendaRequest> parcelas) {
        for (ParcelaVendaRequest p : parcelas) {
            ParcelaVenda parcela = parcelaVendaRepository.save(new ParcelaVenda(lead, p.numero(), p.valor(), p.dataPrevista()));
            ContaPagarReceber conta = contaPagarReceberRepository.save(new ContaPagarReceber(
                    TipoConta.A_RECEBER, "Parcela " + p.numero() + " - " + lead.getNome(), p.valor(), p.dataPrevista(), null));
            parcela.vincularConta(conta);
            parcelaVendaRepository.save(parcela);
        }
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
