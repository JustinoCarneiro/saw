package com.sawhub.hub.comercial;

import com.sawhub.hub.comercial.dto.AvancarLeadRequest;
import com.sawhub.hub.comercial.dto.DashboardComercialResponse;
import com.sawhub.hub.comercial.dto.LeadResponse;
import com.sawhub.hub.comercial.dto.RankingItem;
import com.sawhub.hub.comercial.dto.VendedorResumo;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.ColaboradorRepository;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** H13.1–H13.3 — pipeline, dashboard e ranking comercial. */
@RestController
@RequestMapping("/api/v1/admin/comercial")
@RequiresModulo(Modulo.COMERCIAL)
@Validated
public class ComercialController {

    private final LeadService leadService;
    private final ComercialDashboardService dashboardService;
    private final RankingComercialService rankingService;
    private final ColaboradorRepository colaboradorRepository;

    public ComercialController(LeadService leadService, ComercialDashboardService dashboardService,
                                RankingComercialService rankingService, ColaboradorRepository colaboradorRepository) {
        this.leadService = leadService;
        this.dashboardService = dashboardService;
        this.rankingService = rankingService;
        this.colaboradorRepository = colaboradorRepository;
    }

    /** Só leitura, pro seletor de vendedor na tela de funil (mover lead pra Em contato) —
     * TeamController é gated por Modulo.TIME (só Fundador), então uma área Comercial não-Fundador
     * não conseguiria listar colaboradores por ali. Escopo mínimo: nome/id de quem é da própria
     * área Comercial. */
    @GetMapping("/vendedores")
    public List<VendedorResumo> vendedores() {
        return colaboradorRepository.findAllByAreaOrderByNomeAsc(Area.COMERCIAL).stream()
                .map(VendedorResumo::from)
                .toList();
    }

    @GetMapping("/leads")
    public List<LeadResponse> listarLeads(@RequestParam(required = false) StatusLead status,
                                           @RequestParam(required = false) UUID vendedorId) {
        return leadService.listar(status, vendedorId).stream().map(LeadResponse::from).toList();
    }

    @PatchMapping("/leads/{id}/avancar")
    public LeadResponse avancar(@PathVariable UUID id, @Valid @RequestBody AvancarLeadRequest request) {
        return LeadResponse.from(leadService.avancar(id, request));
    }

    @GetMapping("/dashboard")
    public DashboardComercialResponse dashboard(@RequestParam @Min(2020) int ano, @RequestParam @Min(1) @Max(12) int mes) {
        return dashboardService.dashboard(ano, mes);
    }

    @GetMapping("/ranking")
    public List<RankingItem> ranking(@RequestParam @Min(2020) int ano, @RequestParam @Min(1) @Max(12) int mes) {
        return rankingService.ranking(ano, mes);
    }
}
