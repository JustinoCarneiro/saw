package com.sawhub.hub.evento;

import com.sawhub.hub.evento.dto.EventoMentoradoResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H7.1-H7.2 (M13) — `hasRole("MENTORADO")` já garantido pelo SecurityConfig
 * (`/api/v1/mentorado/**`); mesmo padrão de isolamento do M08-M12. */
@RestController
@RequestMapping("/api/v1/mentorado/eventos")
public class EventoMentoradoController {

    private final EventoMentoradoService eventoMentoradoService;

    public EventoMentoradoController(EventoMentoradoService eventoMentoradoService) {
        this.eventoMentoradoService = eventoMentoradoService;
    }

    @GetMapping
    public List<EventoMentoradoResponse> listar(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @RequestParam(required = false) TipoEvento tipo,
                                                 @RequestParam(required = false) String tema) {
        return eventoMentoradoService.listar(principal.getUsuarioId(), tipo, tema);
    }

    @PostMapping("/{id}/inscricao")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoMentoradoResponse inscrever(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return eventoMentoradoService.inscrever(principal.getUsuarioId(), id);
    }

    @DeleteMapping("/{id}/inscricao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        eventoMentoradoService.cancelar(principal.getUsuarioId(), id);
    }
}
