package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.comercial.dto.AvancarLeadRequest;
import com.sawhub.hub.comercial.dto.CriarLeadRequest;
import com.sawhub.hub.comercial.dto.FecharVendaRequest;
import com.sawhub.hub.comercial.dto.ParcelaVendaRequest;
import com.sawhub.hub.comercial.dto.VendaIngressoRequest;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H1.3 + H13.2. M26 — LeadService ganhou LancamentoFinanceiroRepository/CategoriaFinanceiraRepository
 * (no lugar de ContaPagarReceberRepository) e passou a resolver a categoria financeira de toda
 * venda (parcela ou valor pago no ato), ver ROADMAP.md § "Blueprint (M26)". */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private AtividadeLogService atividadeLogService;
    @Mock
    private ParcelaVendaRepository parcelaVendaRepository;
    @Mock
    private VendaIngressoRepository vendaIngressoRepository;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaFinanceiraRepository;

    private LeadService service() {
        return new LeadService(leadRepository, colaboradorRepository, atividadeLogService,
                parcelaVendaRepository, vendaIngressoRepository, eventoRepository,
                lancamentoFinanceiroRepository, categoriaFinanceiraRepository);
    }

    private static Lead leadEmProposta() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        lead.moverParaProposta();
        return lead;
    }

    private static Colaborador colaborador(UUID id, String nome) {
        Colaborador c = new Colaborador(null, nome, Area.COMERCIAL);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    // M26 — toda venda com valorPagoNoAto/parcela precisa resolver uma CategoriaFinanceira por
    // nome (ver LeadService.resolverCategoriaVenda); stub genérico pros testes que não são sobre
    // esse mapeamento em si.
    private void stubQualquerCategoriaEncontrada() {
        CategoriaFinanceira categoria = new CategoriaFinanceira("Categoria teste", TipoLancamento.RECEITA,
                GrupoDre.RECEITA_BRUTA, null);
        when(categoriaFinanceiraRepository.findByNomeIgnoreCase(anyString())).thenReturn(List.of(categoria));
    }

    @Test
    void criarPersisteLeadEmSolicitacao() {
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarLeadRequest("Maria Souza", "maria@restaurante.com", "11999998888",
                "Quero saber mais");

        Lead lead = service().criar(request);

        assertThat(lead.getStatus()).isEqualTo(StatusLead.SOLICITACAO);
        assertThat(lead.getNome()).isEqualTo("Maria Souza");
    }

    @Test
    void listarDelegaFiltroParaOBancoEmVezDeFiltrarEmMemoria() {
        // Achado M1 da revisão de segurança: o filtro precisa acontecer em SQL (usando os
        // índices idx_lead_status/idx_lead_vendedor), não trazer a tabela inteira pro heap.
        UUID vendedorId = UUID.randomUUID();
        when(leadRepository.buscarComFiltro(StatusLead.PROPOSTA, vendedorId)).thenReturn(List.of());

        service().listar(StatusLead.PROPOSTA, vendedorId);

        verify(leadRepository).buscarComFiltro(StatusLead.PROPOSTA, vendedorId);
    }

    @Test
    void listarSemFiltroPassaNulosAdiante() {
        when(leadRepository.buscarComFiltro(isNull(), isNull())).thenReturn(List.of());

        service().listar(null, null);

        verify(leadRepository).buscarComFiltro(eq((StatusLead) null), eq((UUID) null));
    }

    @Test
    void avancarParaEmContatoAtribuiVendedor() {
        UUID leadId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        Colaborador vendedor = colaborador(vendedorId, "Paula");
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.of(vendedor));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, vendedorId, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.EM_CONTATO);
        assertThat(avancado.getVendedor()).isSameAs(vendedor);
    }

    @Test
    void avancarParaEmContatoSemVendedorLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendedor");
    }

    @Test
    void avancarParaEmContatoComVendedorInexistenteLancaErro() {
        UUID leadId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, vendedorId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void avancarParaPropostaExigeEmContato() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.PROPOSTA, null, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.PROPOSTA);
    }

    @Test
    void avancarParaPerdidoRegistraMotivo() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId,
                new AvancarLeadRequest(StatusLead.PERDIDO, null, "Optou por concorrente"));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.PERDIDO);
        assertThat(avancado.getMotivoPerdido()).isEqualTo("Optou por concorrente");
        verify(atividadeLogService).registrar("LEAD_PERDIDO", "Lead perdido: Maria Souza");
    }

    // M28 — pulando etapa continua protegido pela máquina de estado da entidade (Lead.
    // exigirStatus), independente do caminho legado de FECHADO ter sido removido: SOLICITACAO ->
    // PROPOSTA direto (sem passar por EM_CONTATO) ainda é uma transição inválida.
    @Test
    void avancarPulandoEtapaLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PROPOSTA, null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void avancarLeadInexistenteLancaErro() {
        UUID leadId = UUID.randomUUID();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    // M28 — o caminho legado de fechar um lead por avancar()/FECHADO foi removido junto com Plano
    // ("não existem planos, mas sim produtos"); fechar venda de verdade é só via fecharVenda()
    // (M25). Sempre lança erro, mesmo com o lead numa etapa válida (PROPOSTA) — substitui os
    // testes antigos avancarParaFechadoRegistraPlanoFechado/avancarParaFechadoSemPlanoLancaErro.
    @Test
    void avancarParaFechadoSempreLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        lead.moverParaProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.FECHADO, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechar venda");
        verifyNoInteractions(atividadeLogService);
    }

    @Test
    void avancarParaPerdidoSemMotivoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motivo");
    }

    @Test
    void avancarParaPerdidoComMotivoEmBrancoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motivo");
    }

    // M25 — "formulário único de venda".
    @Test
    void fecharVendaSimplesGravaCamposSemParcelaNemIngresso() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new FecharVendaRequest(ProdutoVenda.CONSULTORIA, OrigemVenda.DIRETA,
                new BigDecimal("9000.00"), new BigDecimal("9000.00"), FormaPagamento.PIX, null, null, null);
        Lead fechado = service().fecharVenda(leadId, request);

        assertThat(fechado.getStatus()).isEqualTo(StatusLead.FECHADO);
        assertThat(fechado.getProdutoVenda()).isEqualTo(ProdutoVenda.CONSULTORIA);
        verify(atividadeLogService).registrar("LEAD_VENDA_FECHADA", "Venda fechada: Maria Souza");
        // M26 — valorPagoNoAto (9000, igual ao total) agora gera um lançamento REALIZADO.
        verify(lancamentoFinanceiroRepository).save(any());
        verifyNoInteractions(parcelaVendaRepository, vendaIngressoRepository, eventoRepository);
    }

    @Test
    void fecharVendaComParcelasCriaLancamentoPraCadaUma() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(parcelaVendaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var parcelas = List.of(
                new ParcelaVendaRequest(1, new BigDecimal("2000.00"), LocalDate.of(2026, 8, 17)),
                new ParcelaVendaRequest(2, new BigDecimal("2000.00"), LocalDate.of(2026, 9, 17)));
        var request = new FecharVendaRequest(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA,
                new BigDecimal("26000.00"), new BigDecimal("6000.00"), FormaPagamento.PIX, parcelas, null, null);

        service().fecharVenda(leadId, request);

        // M26 — 1 lançamento pro valorPagoNoAto + 1 por parcela (2) = 3.
        verify(lancamentoFinanceiroRepository, times(3)).save(any());
        verify(parcelaVendaRepository, times(4)).save(any()); // save + vincularLancamento -> save de novo, por parcela
    }

    @Test
    void fecharVendaDeIngressoSemEventoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new FecharVendaRequest(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA,
                new BigDecimal("300.00"), new BigDecimal("300.00"), FormaPagamento.PIX, null, null, null);

        assertThatThrownBy(() -> service().fecharVenda(leadId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evento");
    }

    @Test
    void fecharVendaDeIngressoSemIngressosLancaErro() {
        UUID leadId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(
                new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, null, Instant.now(), "Recife", null, 100)));

        var request = new FecharVendaRequest(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA,
                new BigDecimal("300.00"), new BigDecimal("300.00"), FormaPagamento.PIX, null, eventoId, null);

        assertThatThrownBy(() -> service().fecharVenda(leadId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ingresso");
    }

    @Test
    void fecharVendaDeIngressoCriaUmaVendaIngressoPorIngresso() {
        UUID leadId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        Evento evento = new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, null, Instant.now(), "Recife", null, 100);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(vendaIngressoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var ingressos = List.of(
                new VendaIngressoRequest(CategoriaIngresso.VIP, "João Comprador", "Financeiro", true,
                        "Restaurante do João Ltda", "81999998888", "joao@restaurante.com"),
                new VendaIngressoRequest(CategoriaIngresso.ESSENCIAL, "Ana Sócia", "Financeiro", false,
                        null, null, null));
        var request = new FecharVendaRequest(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA,
                new BigDecimal("600.00"), new BigDecimal("600.00"), FormaPagamento.PIX, null, eventoId, ingressos);

        service().fecharVenda(leadId, request);

        // Gap 3 (raio-x, 19/07/2026) — nomeEmpresa/telefone/email precisam chegar até a
        // VendaIngresso persistida, não só existir no request.
        ArgumentCaptor<VendaIngresso> captor = ArgumentCaptor.forClass(VendaIngresso.class);
        verify(vendaIngressoRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getNomeEmpresa()).isEqualTo("Restaurante do João Ltda");
        assertThat(captor.getAllValues().get(0).getTelefone()).isEqualTo("81999998888");
        assertThat(captor.getAllValues().get(0).getEmail()).isEqualTo("joao@restaurante.com");
        assertThat(captor.getAllValues().get(1).getNomeEmpresa()).isNull();
        // Achado M2 da revisão de segurança: venda de ingresso precisa ocupar vaga de verdade
        // (mesma invariante já usada em EventoMentoradoService.inscrever(), H7.2) — senão o
        // funil comercial vende ingresso além da capacidade do evento sem qualquer aviso.
        assertThat(evento.getVagasOcupadas()).isEqualTo(2);
        verify(eventoRepository).save(evento);
    }

    @Test
    void fecharVendaDeIngressoSemVagaSuficienteLancaErro() {
        UUID leadId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        Evento evento = new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, null, Instant.now(), "Recife", null, 1);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));

        var ingressos = List.of(
                new VendaIngressoRequest(CategoriaIngresso.VIP, "João Comprador", "Financeiro", true, null, null, null),
                new VendaIngressoRequest(CategoriaIngresso.ESSENCIAL, "Ana Sócia", "Financeiro", false, null, null, null));
        var request = new FecharVendaRequest(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA,
                new BigDecimal("600.00"), new BigDecimal("600.00"), FormaPagamento.PIX, null, eventoId, ingressos);

        assertThatThrownBy(() -> service().fecharVenda(leadId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vaga");
        // @Transactional garante rollback do que já foi salvo (ex.: o 1º ingresso, antes do 2º
        // estourar a vaga) — não testável com mocks, coberto pela infraestrutura de transação.
    }

    @Test
    void fecharVendaDeIngressoParaEventoCanceladoLancaErro() {
        UUID leadId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        Evento evento = new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, null, Instant.now(), "Recife", null, 100);
        evento.cancelar();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));

        var ingressos = List.of(new VendaIngressoRequest(CategoriaIngresso.VIP, "João Comprador", "Financeiro", true, null, null, null));
        var request = new FecharVendaRequest(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA,
                new BigDecimal("300.00"), new BigDecimal("300.00"), FormaPagamento.PIX, null, eventoId, ingressos);

        assertThatThrownBy(() -> service().fecharVenda(leadId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evento");
        verifyNoInteractions(vendaIngressoRepository);
    }

    @Test
    void fecharVendaComValorPagoNoAtoMaiorQueOTotalLancaErro() {
        // Achado B3 da revisão de segurança: sem esta checagem dá pra registrar uma venda com
        // valor pago no ato maior que o valor total, corrompendo silenciosamente o financeiro.
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        var request = new FecharVendaRequest(ProdutoVenda.CONSULTORIA, OrigemVenda.DIRETA,
                new BigDecimal("1000.00"), new BigDecimal("1500.00"), FormaPagamento.PIX, null, null, null);

        assertThatThrownBy(() -> service().fecharVenda(leadId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pago no ato");
        verifyNoInteractions(parcelaVendaRepository, vendaIngressoRepository, lancamentoFinanceiroRepository);
    }

    // Gap 7 (raio-x + pesquisa da taxa real da Hotmart, confirmado 19/07/2026).
    @Test
    void fecharVendaComTaxaPlataformaRetidaGravaOTerceiroConceito() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new FecharVendaRequest(ProdutoVenda.PRODUTO_DIGITAL, OrigemVenda.HOTMART,
                new BigDecimal("1000.00"), new BigDecimal("890.00"), FormaPagamento.HOTMART, null, null, null,
                new BigDecimal("110.00"));
        Lead fechado = service().fecharVenda(leadId, request);

        assertThat(fechado.getTaxaPlataformaRetida()).isEqualByComparingTo("110.00");
        assertThat(fechado.getValorPagoNoAto()).isEqualByComparingTo("890.00");
    }

    @Test
    void fecharVendaComPagoMaisTaxaMaiorQueOTotalLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        // 890 (pago) + 200 (taxa) = 1090 > 1000 (total) — mesma invariante de antes, agora com o
        // terceiro conceito somado.
        var request = new FecharVendaRequest(ProdutoVenda.PRODUTO_DIGITAL, OrigemVenda.HOTMART,
                new BigDecimal("1000.00"), new BigDecimal("890.00"), FormaPagamento.HOTMART, null, null, null,
                new BigDecimal("200.00"));

        assertThatThrownBy(() -> service().fecharVenda(leadId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pago no ato")
                .hasMessageContaining("taxa de plataforma");
        verifyNoInteractions(parcelaVendaRepository, vendaIngressoRepository, lancamentoFinanceiroRepository);
    }

    @Test
    void fecharVendaComPagoMaisTaxaIgualAoTotalNaoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new FecharVendaRequest(ProdutoVenda.PRODUTO_DIGITAL, OrigemVenda.HOTMART,
                new BigDecimal("1000.00"), new BigDecimal("890.00"), FormaPagamento.HOTMART, null, null, null,
                new BigDecimal("110.00"));

        Lead fechado = service().fecharVenda(leadId, request);

        assertThat(fechado.getStatus()).isEqualTo(StatusLead.FECHADO);
    }

    // Gap 7 (taxaPlataformaRetida no DRE, confirmado 19/07/2026) — Receita Bruta lançada passa a
    // ser o valor cheio da venda (pago no ato + taxa), com a taxa virando um 2º lançamento de
    // DESPESA (DEDUCOES) separado, pra Receita Líquida do DRE bater com o que a SAW recebeu.
    @Test
    void fecharVendaComTaxaPlataformaRetidaGeraReceitaBrutaEDeducaoSeparada() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new FecharVendaRequest(ProdutoVenda.PRODUTO_DIGITAL, OrigemVenda.HOTMART,
                new BigDecimal("1000.00"), new BigDecimal("890.00"), FormaPagamento.HOTMART, null, null, null,
                new BigDecimal("110.00"));
        service().fecharVenda(leadId, request);

        ArgumentCaptor<LancamentoFinanceiro> captor = ArgumentCaptor.forClass(LancamentoFinanceiro.class);
        verify(lancamentoFinanceiroRepository, times(2)).save(captor.capture());
        LancamentoFinanceiro receita = captor.getAllValues().get(0);
        LancamentoFinanceiro deducao = captor.getAllValues().get(1);
        assertThat(receita.getTipo()).isEqualTo(TipoLancamento.RECEITA);
        // Receita Bruta = 890 (pago no ato) + 110 (taxa retida) = 1000, não só o líquido de 890.
        assertThat(receita.getValor()).isEqualByComparingTo("1000.00");
        assertThat(deducao.getTipo()).isEqualTo(TipoLancamento.DESPESA);
        assertThat(deducao.getValor()).isEqualByComparingTo("110.00");
    }

    // Sem taxa de plataforma, o comportamento de sempre continua: só 1 lançamento, valor igual ao
    // pago no ato (não regride o caminho comum, não-Hotmart).
    @Test
    void fecharVendaSemTaxaPlataformaNaoGeraLancamentoDeDeducao() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new FecharVendaRequest(ProdutoVenda.CONSULTORIA, OrigemVenda.DIRETA,
                new BigDecimal("9000.00"), new BigDecimal("9000.00"), FormaPagamento.PIX, null, null, null);
        service().fecharVenda(leadId, request);

        ArgumentCaptor<LancamentoFinanceiro> captor = ArgumentCaptor.forClass(LancamentoFinanceiro.class);
        verify(lancamentoFinanceiroRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("9000.00");
    }

    // Gap 6 (Pix Recorrente, confirmado 19/07/2026) — o lançamento de receita nasce marcado como
    // pagamentoRecorrente=true, sinal que RelatorioFinanceiroService.dashboardFaturamento soma no
    // MRR independente da categoria do produto vendido.
    @Test
    void fecharVendaComPixRecorrenteMarcaLancamentoComoRecorrente() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubQualquerCategoriaEncontrada();
        when(lancamentoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new FecharVendaRequest(ProdutoVenda.CONSULTORIA, OrigemVenda.DIRETA,
                new BigDecimal("9000.00"), new BigDecimal("9000.00"), FormaPagamento.PIX_RECORRENTE, null, null, null);
        service().fecharVenda(leadId, request);

        ArgumentCaptor<LancamentoFinanceiro> captor = ArgumentCaptor.forClass(LancamentoFinanceiro.class);
        verify(lancamentoFinanceiroRepository).save(captor.capture());
        assertThat(captor.getValue().isPagamentoRecorrente()).isTrue();
    }

    // M26 — mapeamento ProdutoVenda→CategoriaFinanceira: se a categoria esperada não estiver
    // pré-cadastrada (deveria vir da migration V40), falha alto e claro em vez de silenciosamente
    // deixar a venda sem rastro financeiro.
    @Test
    void fecharVendaComCategoriaEsperadaNaoEncontradaLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoriaFinanceiraRepository.findByNomeIgnoreCase("Consultoria")).thenReturn(List.of());

        var request = new FecharVendaRequest(ProdutoVenda.CONSULTORIA, OrigemVenda.DIRETA,
                new BigDecimal("9000.00"), new BigDecimal("9000.00"), FormaPagamento.PIX, null, null, null);

        assertThatThrownBy(() -> service().fecharVenda(leadId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Consultoria");
    }
}
