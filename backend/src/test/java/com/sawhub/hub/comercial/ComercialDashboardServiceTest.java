package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.RelatorioFinanceiroService;
import com.sawhub.hub.financeiro.dto.ComposicaoReceita;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** H13.1 — RED primeiro: ComercialDashboardService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class ComercialDashboardServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private RelatorioFinanceiroService relatorioFinanceiroService;

    private ComercialDashboardService service() {
        return new ComercialDashboardService(leadRepository, relatorioFinanceiroService);
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
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.PROPOSTA, 3),
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.FECHADO, 4),
                new com.sawhub.hub.comercial.dto.FunilItem(StatusLead.PERDIDO, 2));
    }

    @Test
    void dashboardCalculaNovosMentoradosETaxaDeConversao() {
        stubFunilVazio();
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.FECHADO), any(), any())).thenReturn(4L);
        when(leadRepository.countByStatusAndDataFechamentoBetween(eq(StatusLead.PERDIDO), any(), any())).thenReturn(2L);
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 7)).thenReturn(faturamento(new BigDecimal("21400.00"), null));
        when(relatorioFinanceiroService.dashboardFaturamento(2026, 6)).thenReturn(faturamento(new BigDecimal("21400.00"), null));

        DashboardComercialResponse dashboard = service().dashboard(2026, 7);

        assertThat(dashboard.novosMentoradosNoMes()).isEqualTo(4);
        assertThat(dashboard.taxaConversaoPct()).isCloseTo(66.7, within(0.1));
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
