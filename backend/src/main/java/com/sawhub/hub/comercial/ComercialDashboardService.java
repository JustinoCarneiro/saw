package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.comercial.dto.FunilItem;
import com.sawhub.hub.common.VariacaoCalculator;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.RelatorioFinanceiroService;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/** H13.1 — dashboard comercial. MRR e vendas da loja não são dado próprio: lêem direto do
 * Financeiro (mesmo padrão de {@code consolidated/} sobre {@code mentorado/}), não duplicam a
 * agregação que {@link RelatorioFinanceiroService} já faz. */
@Service
public class ComercialDashboardService {

    private final LeadRepository leadRepository;
    private final RelatorioFinanceiroService relatorioFinanceiroService;

    public ComercialDashboardService(LeadRepository leadRepository, RelatorioFinanceiroService relatorioFinanceiroService) {
        this.leadRepository = leadRepository;
        this.relatorioFinanceiroService = relatorioFinanceiroService;
    }

    public DashboardComercialResponse dashboard(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        Instant inicio = periodo.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fim = periodo.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long fechados = leadRepository.countByStatusAndDataFechamentoBetween(StatusLead.FECHADO, inicio, fim);
        long perdidos = leadRepository.countByStatusAndDataFechamentoBetween(StatusLead.PERDIDO, inicio, fim);
        double taxaConversaoPct = (fechados + perdidos) == 0 ? 0.0
                : BigDecimal.valueOf(fechados)
                        .divide(BigDecimal.valueOf(fechados + perdidos), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        List<FunilItem> funil = Arrays.stream(StatusLead.values())
                .map(status -> new FunilItem(status, leadRepository.countByStatus(status)))
                .toList();

        DashboardFaturamentoResponse atual = relatorioFinanceiroService.dashboardFaturamento(ano, mes);
        YearMonth anterior = periodo.minusMonths(1);
        DashboardFaturamentoResponse doMesAnterior = relatorioFinanceiroService.dashboardFaturamento(
                anterior.getYear(), anterior.getMonthValue());

        BigDecimal vendasLoja = vendasPorOrigem(atual, OrigemReceita.LOJA);
        double variacaoMrrPct = VariacaoCalculator.pct(doMesAnterior.mrr(), atual.mrr());

        return new DashboardComercialResponse(fechados, taxaConversaoPct, atual.mrr(), vendasLoja, variacaoMrrPct, funil);
    }

    private static BigDecimal vendasPorOrigem(DashboardFaturamentoResponse faturamento, OrigemReceita origem) {
        return faturamento.composicao().stream()
                .filter(c -> c.origem() == origem)
                .map(c -> c.valor())
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

}
