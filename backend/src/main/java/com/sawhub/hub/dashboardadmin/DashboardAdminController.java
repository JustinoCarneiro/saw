package com.sawhub.hub.dashboardadmin;

import com.sawhub.hub.dashboardadmin.dto.DashboardAdminResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** H10.1–H10.3 — painel geral do Admin, substitui o `PlaceholderScreen` que ocupava
 * `/admin/dashboard` (`Modulo.DASHBOARD` já existia como scaffolding, ver Blueprint M16). */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiresModulo(Modulo.DASHBOARD)
@Validated
public class DashboardAdminController {

    private final DashboardAdminService dashboardAdminService;

    public DashboardAdminController(DashboardAdminService dashboardAdminService) {
        this.dashboardAdminService = dashboardAdminService;
    }

    @GetMapping
    public DashboardAdminResponse resumo(@RequestParam @Min(2020) int ano, @RequestParam @Min(1) @Max(12) int mes) {
        return dashboardAdminService.resumo(ano, mes);
    }
}
