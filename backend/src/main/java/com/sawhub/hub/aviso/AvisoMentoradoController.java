package com.sawhub.hub.aviso;

import com.sawhub.hub.aviso.dto.AvisoMentoradoResponse;
import com.sawhub.hub.aviso.dto.ResumoAvisosResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** H12.1 — `hasRole("MENTORADO")` já garantido pelo SecurityConfig (`/api/v1/mentorado/**`); id
 * do {@link com.sawhub.hub.mentorado.Mentorado} nunca vem de parâmetro de request, só do usuário
 * autenticado (mesmo padrão de M08-M16). */
@RestController
@RequestMapping("/api/v1/mentorado/avisos")
public class AvisoMentoradoController {

    private final AvisoMentoradoService avisoMentoradoService;

    public AvisoMentoradoController(AvisoMentoradoService avisoMentoradoService) {
        this.avisoMentoradoService = avisoMentoradoService;
    }

    @GetMapping
    public List<AvisoMentoradoResponse> listar(@AuthenticationPrincipal AppUserPrincipal principal,
                                                @RequestParam(required = false) CategoriaAviso categoria,
                                                @RequestParam(required = false) Boolean apenasNaoLidos) {
        return avisoMentoradoService.listar(principal.getUsuarioId(), categoria, apenasNaoLidos);
    }

    @GetMapping("/resumo")
    public ResumoAvisosResponse resumo(@AuthenticationPrincipal AppUserPrincipal principal) {
        return avisoMentoradoService.resumo(principal.getUsuarioId());
    }

    @PatchMapping("/{id}/lido")
    public void marcarLido(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        avisoMentoradoService.marcarLido(principal.getUsuarioId(), id);
    }

    @PatchMapping("/marcar-todos-lidos")
    public void marcarTodosLidos(@AuthenticationPrincipal AppUserPrincipal principal) {
        avisoMentoradoService.marcarTodosLidos(principal.getUsuarioId());
    }
}
