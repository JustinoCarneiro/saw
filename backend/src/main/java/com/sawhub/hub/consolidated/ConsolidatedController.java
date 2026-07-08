package com.sawhub.hub.consolidated;

import com.sawhub.hub.consolidated.dto.ConsolidatedSummaryResponse;
import com.sawhub.hub.consolidated.dto.MentoradoConsolidadoResponse;
import com.sawhub.hub.consolidated.dto.RankingFaturamentoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/consolidated")
@RequiresModulo(Modulo.PAINEL_CONSOLIDADO)
public class ConsolidatedController {

    private final ConsolidatedService consolidatedService;

    public ConsolidatedController(ConsolidatedService consolidatedService) {
        this.consolidatedService = consolidatedService;
    }

    @GetMapping("/mentorados")
    public List<MentoradoConsolidadoResponse> mentorados() {
        return consolidatedService.listarMentorados();
    }

    @GetMapping("/summary")
    public ConsolidatedSummaryResponse summary() {
        return consolidatedService.resumo();
    }

    @GetMapping("/ranking-faturamento")
    public List<RankingFaturamentoResponse> ranking(@RequestParam(defaultValue = "3") int top) {
        return consolidatedService.rankingFaturamento(top);
    }
}
