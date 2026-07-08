package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import com.sawhub.hub.financeiro.dto.DreResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/financeiro")
@RequiresModulo(Modulo.FINANCEIRO)
@Validated
public class RelatorioFinanceiroController {

    private final RelatorioFinanceiroService relatorioService;

    public RelatorioFinanceiroController(RelatorioFinanceiroService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/dre")
    public DreResponse dre(@RequestParam @Min(2020) int ano, @RequestParam @Min(1) @Max(12) int mes) {
        return relatorioService.dre(ano, mes);
    }

    @GetMapping("/dashboard-faturamento")
    public DashboardFaturamentoResponse dashboardFaturamento(@RequestParam @Min(2020) int ano,
                                                               @RequestParam @Min(1) @Max(12) int mes) {
        return relatorioService.dashboardFaturamento(ano, mes);
    }
}
