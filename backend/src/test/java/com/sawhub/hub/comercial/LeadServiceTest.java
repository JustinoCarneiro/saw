package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.sawhub.hub.financeiro.ContaPagarReceberRepository;
import com.sawhub.hub.mentorado.Plano;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H1.3 + H13.2 — RED primeiro: LeadService ainda não existe neste ponto do ciclo. */
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
    private ContaPagarReceberRepository contaPagarReceberRepository;

    private LeadService service() {
        return new LeadService(leadRepository, colaboradorRepository, atividadeLogService,
                parcelaVendaRepository, vendaIngressoRepository, eventoRepository, contaPagarReceberRepository);
    }

    private static Lead leadEmProposta() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        lead.moverParaProposta();
        return lead;
    }

    private static Colaborador colaborador(UUID id, String nome) {
        Colaborador c = new Colaborador(null, nome, Area.COMERCIAL);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void criarPersisteLeadEmSolicitacao() {
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarLeadRequest("Maria Souza", "maria@restaurante.com", "11999998888",
                "Quero saber mais", Plano.ESSENCIAL);

        Lead lead = service().criar(request);

        assertThat(lead.getStatus()).isEqualTo(StatusLead.SOLICITACAO);
        assertThat(lead.getNome()).isEqualTo("Maria Souza");
        assertThat(lead.getPlanoInteresse()).isEqualTo(Plano.ESSENCIAL);
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
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        Colaborador vendedor = colaborador(vendedorId, "Paula");
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.of(vendedor));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, vendedorId, null, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.EM_CONTATO);
        assertThat(avancado.getVendedor()).isSameAs(vendedor);
    }

    @Test
    void avancarParaEmContatoSemVendedorLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendedor");
    }

    @Test
    void avancarParaEmContatoComVendedorInexistenteLancaErro() {
        UUID leadId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(colaboradorRepository.findById(vendedorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.EM_CONTATO, vendedorId, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void avancarParaPropostaExigeEmContato() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.PROPOSTA, null, null, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.PROPOSTA);
    }

    @Test
    void avancarParaFechadoRegistraPlanoFechado() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        lead.moverParaProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId, new AvancarLeadRequest(StatusLead.FECHADO, null, Plano.ESSENCIAL, null));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.FECHADO);
        assertThat(avancado.getPlanoFechado()).isEqualTo(Plano.ESSENCIAL);
        assertThat(avancado.getDataFechamento()).isNotNull();
        verify(atividadeLogService).registrar("LEAD_FECHADO", "Lead fechado: Maria Souza");
    }

    @Test
    void avancarParaPerdidoRegistraMotivo() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead avancado = service().avancar(leadId,
                new AvancarLeadRequest(StatusLead.PERDIDO, null, null, "Optou por concorrente"));

        assertThat(avancado.getStatus()).isEqualTo(StatusLead.PERDIDO);
        assertThat(avancado.getMotivoPerdido()).isEqualTo("Optou por concorrente");
        verify(atividadeLogService).registrar("LEAD_PERDIDO", "Lead perdido: Maria Souza");
    }

    @Test
    void avancarPulandoEtapaLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.FECHADO, null, Plano.ESSENCIAL, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void avancarLeadInexistenteLancaErro() {
        UUID leadId = UUID.randomUUID();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, null, "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void avancarParaFechadoSemPlanoLancaErro() {
        // Achado L2 da revisão de segurança: sem esta checagem, um FECHADO sem plano quebra a
        // atribuição de "vendas por plano" (H13.1) silenciosamente.
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        lead.moverParaEmContato(colaborador(UUID.randomUUID(), "Paula"));
        lead.moverParaProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.FECHADO, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plano");
    }

    @Test
    void avancarParaPerdidoSemMotivoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motivo");
    }

    @Test
    void avancarParaPerdidoComMotivoEmBrancoLancaErro() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service().avancar(leadId, new AvancarLeadRequest(StatusLead.PERDIDO, null, null, "   ")))
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

        var request = new FecharVendaRequest(ProdutoVenda.CONSULTORIA, OrigemVenda.DIRETA,
                new BigDecimal("9000.00"), new BigDecimal("9000.00"), FormaPagamento.PIX, null, null, null);
        Lead fechado = service().fecharVenda(leadId, request);

        assertThat(fechado.getStatus()).isEqualTo(StatusLead.FECHADO);
        assertThat(fechado.getProdutoVenda()).isEqualTo(ProdutoVenda.CONSULTORIA);
        verify(atividadeLogService).registrar("LEAD_VENDA_FECHADA", "Venda fechada: Maria Souza");
        verifyNoInteractions(parcelaVendaRepository, vendaIngressoRepository, eventoRepository, contaPagarReceberRepository);
    }

    @Test
    void fecharVendaComParcelasCriaContaPagarReceberPraCadaUma() {
        UUID leadId = UUID.randomUUID();
        Lead lead = leadEmProposta();
        when(leadRepository.buscarPorIdComVendedor(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contaPagarReceberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(parcelaVendaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var parcelas = List.of(
                new ParcelaVendaRequest(1, new BigDecimal("2000.00"), LocalDate.of(2026, 8, 17)),
                new ParcelaVendaRequest(2, new BigDecimal("2000.00"), LocalDate.of(2026, 9, 17)));
        var request = new FecharVendaRequest(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA,
                new BigDecimal("26000.00"), new BigDecimal("6000.00"), FormaPagamento.PIX, parcelas, null, null);

        service().fecharVenda(leadId, request);

        verify(contaPagarReceberRepository, times(2)).save(any());
        verify(parcelaVendaRepository, org.mockito.Mockito.times(4)).save(any()); // save + vincularConta -> save de novo, por parcela
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

        var ingressos = List.of(
                new VendaIngressoRequest(CategoriaIngresso.VIP, "João Comprador", "Financeiro", true),
                new VendaIngressoRequest(CategoriaIngresso.ESSENCIAL, "Ana Sócia", "Financeiro", false));
        var request = new FecharVendaRequest(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA,
                new BigDecimal("600.00"), new BigDecimal("600.00"), FormaPagamento.PIX, null, eventoId, ingressos);

        service().fecharVenda(leadId, request);

        verify(vendaIngressoRepository, times(2)).save(any());
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
                new VendaIngressoRequest(CategoriaIngresso.VIP, "João Comprador", "Financeiro", true),
                new VendaIngressoRequest(CategoriaIngresso.ESSENCIAL, "Ana Sócia", "Financeiro", false));
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

        var ingressos = List.of(new VendaIngressoRequest(CategoriaIngresso.VIP, "João Comprador", "Financeiro", true));
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
        verifyNoInteractions(parcelaVendaRepository, vendaIngressoRepository, contaPagarReceberRepository);
    }
}
