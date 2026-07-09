package com.sawhub.hub.mentorado;

import com.sawhub.hub.mentorado.dto.DashboardMentoradoResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** H2.1–H2.3 (M08) — `hasRole("MENTORADO")` já garantido pelo SecurityConfig
 * (`/api/v1/mentorado/**`); o id do {@link Mentorado} nunca vem de parâmetro de request, só do
 * usuário autenticado — isolamento por tenant garantido por construção, não por checagem manual. */
@RestController
@RequestMapping("/api/v1/mentorado")
public class MentoradoDashboardController {

    private final MentoradoDashboardService mentoradoDashboardService;

    public MentoradoDashboardController(MentoradoDashboardService mentoradoDashboardService) {
        this.mentoradoDashboardService = mentoradoDashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardMentoradoResponse dashboard(@AuthenticationPrincipal AppUserPrincipal principal) {
        return mentoradoDashboardService.dashboard(principal.getUsuarioId());
    }
}
