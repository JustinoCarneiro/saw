package com.sawhub.hub.financeiro;

import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import com.sawhub.hub.financeiro.dto.DreResponse;
import com.sawhub.hub.financeiro.dto.EventoResumoFinanceiro;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
    private final EventoRepository eventoRepository;

    public RelatorioFinanceiroController(RelatorioFinanceiroService relatorioService, EventoRepository eventoRepository) {
        this.relatorioService = relatorioService;
        this.eventoRepository = eventoRepository;
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

    // Change request 17/07/2026 ("evento no financeiro") — só leitura, pro seletor de evento em
    // Nova conta/filtro de Contas. Mesmo raciocínio de ComercialController.eventosParaVenda():
    // EventoController "geral" é gated por Modulo.CONTEUDOS, uma área Financeiro não-Fundador não
    // conseguiria listar eventos por ali. Sem filtro de status de propósito (ver EventoResumoFinanceiro).
    @GetMapping("/eventos")
    public List<EventoResumoFinanceiro> eventos() {
        return eventoRepository.buscarComFiltro(null, null).stream().map(EventoResumoFinanceiro::from).toList();
    }
}
