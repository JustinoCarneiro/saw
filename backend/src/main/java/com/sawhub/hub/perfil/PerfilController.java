package com.sawhub.hub.perfil;

import com.sawhub.hub.perfil.dto.AssinaturaResponse;
import com.sawhub.hub.perfil.dto.AtualizarPerfilMentoradoRequest;
import com.sawhub.hub.perfil.dto.JornadaResponse;
import com.sawhub.hub.perfil.dto.PerfilMentoradoResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** H9.1–H9.3 (M15) — `hasRole("MENTORADO")` já garantido pelo SecurityConfig
 * (`/api/v1/mentorado/**`); id do {@link com.sawhub.hub.mentorado.Mentorado} nunca vem de
 * parâmetro de request, só do usuário autenticado (mesmo padrão de M08-M14). */
@RestController
@RequestMapping("/api/v1/mentorado/perfil")
public class PerfilController {

    private final PerfilMentoradoService perfilMentoradoService;
    private final PerfilJornadaService perfilJornadaService;

    public PerfilController(PerfilMentoradoService perfilMentoradoService, PerfilJornadaService perfilJornadaService) {
        this.perfilMentoradoService = perfilMentoradoService;
        this.perfilJornadaService = perfilJornadaService;
    }

    @GetMapping
    public PerfilMentoradoResponse buscar(@AuthenticationPrincipal AppUserPrincipal principal) {
        return perfilMentoradoService.buscar(principal.getUsuarioId());
    }

    @PatchMapping
    public PerfilMentoradoResponse atualizar(@AuthenticationPrincipal AppUserPrincipal principal,
                                              @Valid @RequestBody AtualizarPerfilMentoradoRequest request) {
        return perfilMentoradoService.atualizar(principal.getUsuarioId(), request);
    }

    @GetMapping("/jornada")
    public JornadaResponse jornada(@AuthenticationPrincipal AppUserPrincipal principal) {
        return perfilJornadaService.jornada(principal.getUsuarioId());
    }

    @GetMapping("/assinatura")
    public AssinaturaResponse assinatura(@AuthenticationPrincipal AppUserPrincipal principal) {
        return perfilMentoradoService.assinatura(principal.getUsuarioId());
    }
}
