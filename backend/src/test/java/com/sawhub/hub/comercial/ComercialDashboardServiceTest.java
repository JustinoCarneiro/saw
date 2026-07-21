package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.evento.TipoEvento;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.RelatorioFinanceiroService;
import com.sawhub.hub.financeiro.dto.ComposicaoReceita;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H13.1 — RED primeiro: ComercialDashboardService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class ComercialDashboardServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private RelatorioFinanceiroService relatorioFinanceiroService;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private VendaIngressoRepository vendaIngressoRepository;

    private ComercialDashboardService service() {
        return new ComercialDashboardService(leadRepository, relatorioFinanceiroService, eventoRepository, vendaIngressoRepository);
    }

    private static DashboardFaturamentoResponse faturamento(BigDecimal mrr, BigDecimal vendasLoja) {
        List<ComposicaoReceita> composicao = vendasLoja == null
                ? List.of(new ComposicaoReceita(OrigemReceita.ASSINATURA, mrr))
                : List.of(new ComposicaoReceita(OrigemReceita.ASSINATURA, mrr), new ComposicaoReceita(OrigemReceita.LOJA, vendasLoja));
        return new DashboardFaturamentoResponse(mrr.add(vendasLoja == null ? BigDecimal.ZERO : vendasLoja), mrr, 0.0, composicao);
    }

    @Test
    void dashboardMontaFunilComTodosOsStatus() {
        when(leadRepository.countByStatus(StatusLead.SOLICITACAO)).thenReturn(12L);
        when(leadRepository.countByStatus(StatusLead.EM_CONTATO)).thenReturn(5L);
        // M25 — DIAGNOSTICO é etapa nova do funil (aditiva); precisa de stub igual às demais.
        when(leadRepository.countByStatus(StatusLead.DIAGNOSTICO)).thenReturn(1L);
        when(leadRepository.countByStatus(StatusLead.PROPOSTA)).thenReturn(3L);
        when(leadRepository.countByStatus(StatusLead.FECHADO)).thenReturn(4L);
        when(leadRepository.countByStatus(StatusLead.PERDIDO)).thenReturn(2L);
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.FECHADO), any(), any())).thenReturn(4L);
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.PERDIDO), any(), any())).thenReturn(2L);
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(new BigDecimal("21400.00"), new BigDecimal("5100.00")));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(new BigDecimal("19700.00"), null));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.funil()).containsExactlyInAnyOrder(
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.SOLICITACAO, 12),
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.EM_CONTATO, 5),
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.DIAGNOSTICO, 1),
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.PROPOSTA, 3),
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.FECHADO, 4),
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.PERDIDO, 2));
    }

    @Test
    void dashboardCalculaNovosMentoradosETaxaDeConversao() {
        stubFunilVazio();
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.FECHADO), any(), any())).thenReturn(4L);
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.PERDIDO), any(), any())).thenReturn(2L);
        when(leadRepository.countByStatusAndDataFechamentoBetweenExcluindoProduto(
                eq(StatusLead.FECHADO), any(), any(), eq(ProdutoVenda.INGRESSO_EVENTO))).thenReturn(4L);
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(new BigDecimal("21400.00"), null));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(new BigDecimal("21400.00"), null));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.novosMentoradosNoMes()).isEqualTo(4);
        assertThat(dashboard.taxaConversaoPct()).isCloseTo(66.7, within(0.1));
    }

    // M25 (Suposição 7) — "novos mentorados no mês" exclui venda de ingresso (contabilizada à
    // parte em vendaIngressos), mas a taxa de conversão continua contando qualquer venda fechada
    // como conversão — só o rótulo "novo mentorado" que não se aplica a quem comprou ingresso.
    @Test
    void dashboardNovosMentoradosNoMesExcluiVendaDeIngressoMasConversaoContaTudo() {
        stubFunilVazio();
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.FECHADO), any(), any())).thenReturn(5L);
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.PERDIDO), any(), any())).thenReturn(0L);
        when(leadRepository.countByStatusAndDataFechamentoBetweenExcluindoProduto(
                eq(StatusLead.FECHADO), any(), any(), eq(ProdutoVenda.INGRESSO_EVENTO))).thenReturn(3L);
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(BigDecimal.ZERO, null));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(BigDecimal.ZERO, null));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.novosMentoradosNoMes()).isEqualTo(3);
        assertThat(dashboard.taxaConversaoPct()).isEqualTo(100.0);
    }

    @Test
    void dashboardSemFechamentosNemPerdasNoPeriodoNaoDivideParZero() {
        stubFunilVazio();
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.FECHADO), any(), any())).thenReturn(0L);
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.PERDIDO), any(), any())).thenReturn(0L);
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(BigDecimal.ZERO, null));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(BigDecimal.ZERO, null));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.taxaConversaoPct()).isZero();
    }

    @Test
    void dashboardReaproveitaMrrEVendasLojaDoFinanceiro() {
        stubFunilVazio();
        stubConversaoVazia();
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(new BigDecimal("21400.00"), new BigDecimal("5100.00")));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(new BigDecimal("19700.00"), null));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.mrr()).isEqualByComparingTo("21400.00");
        assertThat(dashboard.vendasLoja()).isEqualByComparingTo("5100.00");
        assertThat(dashboard.variacaoMrrPct()).isCloseTo(8.63, within(0.1));
    }

    @Test
    void dashboardVendasLojaZeroQuandoFinanceiroNaoTemOrigemLoja() {
        stubFunilVazio();
        stubConversaoVazia();
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(new BigDecimal("21400.00"), null));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(new BigDecimal("21400.00"), null));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.vendasLoja()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // M25 (Suposição 7) — venda de ingresso contabilizada por evento REALIZADO no período,
    // deduplicando o valor por Lead (uma venda pode conter vários ingressos do mesmo Lead).
    @Test
    void dashboardMontaVendaIngressosPorEventoRealizadoNoPeriodoDeduplicandoValorPorLead() {
        stubFunilVazio();
        stubConversaoVazia();
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(BigDecimal.ZERO, null));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(BigDecimal.ZERO, null));

        Evento evento = new Evento("Encontro Nacional SAW 2026", TipoEvento.AO_VIVO, null,
                Instant.parse("2026-07-10T19:00:00Z"), null, null, 200);
        evento.iniciar();
        evento.finalizar();
        UUID eventoId = UUID.randomUUID();
        ReflectionTestUtils.setField(evento, "id", eventoId);
        when(eventoRepository.buscarPorStatusEDataHoraBetween(eq(StatusEvento.REALIZADO), any(), any()))
                .thenReturn(List.of(evento));

        Lead lead = new Lead("João Comprador", "joao@example.com", null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.INGRESSO_EVENTO, OrigemVenda.DIRETA, new BigDecimal("600.00"),
                new BigDecimal("600.00"), FormaPagamento.PIX);
        VendaIngresso ingresso1 = new VendaIngresso(lead, evento, CategoriaIngresso.VIP, "João Comprador", null, true,
                null, null, null);
        VendaIngresso ingresso2 = new VendaIngresso(lead, evento, CategoriaIngresso.VIP, "Ana Sócia", null, false,
                null, null, null);
        when(vendaIngressoRepository.buscarPorEventoIdComLead(eventoId)).thenReturn(List.of(ingresso1, ingresso2));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.vendaIngressos()).hasSize(1);
        var resumo = dashboard.vendaIngressos().get(0);
        assertThat(resumo.eventoId()).isEqualTo(eventoId);
        assertThat(resumo.eventoTitulo()).isEqualTo("Encontro Nacional SAW 2026");
        assertThat(resumo.quantidadeVendida()).isEqualTo(2);
        assertThat(resumo.quantidadeTotal()).isEqualTo(200);
        assertThat(resumo.valorLiquido()).isEqualByComparingTo("600.00");
    }

    private void stubFunilVazio() {
        for (StatusLead status : StatusLead.values()) {
            when(leadRepository.countByStatus(status)).thenReturn(0L);
        }
    }

    private void stubConversaoVazia() {
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.FECHADO), any(), any())).thenReturn(0L);
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.PERDIDO), any(), any())).thenReturn(0L);
    }

    private static java.time.Instant any() {
        return org.mockito.ArgumentMatchers.any(java.time.Instant.class);
    }
}
