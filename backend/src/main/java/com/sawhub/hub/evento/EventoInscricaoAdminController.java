package com.sawhub.hub.evento;

import com.sawhub.hub.evento.dto.EventoInscricaoAdminResponse;
import com.sawhub.hub.evento.dto.EventoMentoradoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** M28 (change request, 21/07/2026) — "controle de vagas em evento por mentorado da Contínua":
 * inscrição/cancelamento pelo time interno, em nome do mentorado. Achado da investigação: antes
 * disto só existia o self-service em {@link EventoMentoradoController} (mentee-facing), hoje
 * inalcançável ({@code AREA_MENTORADO_PAUSADA}) — sem este controller não haveria NENHUM jeito de
 * exercitar a cota na prática. Gated por {@code Modulo.MENTORADOS} (não {@code CONTEUDOS}, que é
 * o módulo de {@link EventoController}): é uma ação sobre o mentorado, não sobre o cadastro do
 * evento em si — mesmo critério já usado pra outras ações de Mentorado dentro do módulo Gestão de
 * Performance (CLAUDE.md § E15). */
@RestController
@RequestMapping("/api/v1/admin/mentorados/{mentoradoId}/eventos")
@RequiresModulo(Modulo.MENTORADOS)
public class EventoInscricaoAdminController {

    private final EventoMentoradoService eventoMentoradoService;

    public EventoInscricaoAdminController(EventoMentoradoService eventoMentoradoService) {
        this.eventoMentoradoService = eventoMentoradoService;
    }

    @GetMapping("/inscricoes")
    public List<EventoInscricaoAdminResponse> listarInscricoes(@PathVariable UUID mentoradoId) {
        return eventoMentoradoService.listarInscricoesAdmin(mentoradoId).stream()
                .map(EventoInscricaoAdminResponse::from)
                .toList();
    }

    @GetMapping("/cota")
    public EventoMentoradoService.CotaEventosInfo consultarCota(@PathVariable UUID mentoradoId) {
        return eventoMentoradoService.consultarCotaAdmin(mentoradoId);
    }

    @PostMapping("/{eventoId}/inscricao")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoMentoradoResponse inscrever(@PathVariable UUID mentoradoId, @PathVariable UUID eventoId) {
        return eventoMentoradoService.inscreverAdmin(mentoradoId, eventoId);
    }

    @DeleteMapping("/{eventoId}/inscricao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID mentoradoId, @PathVariable UUID eventoId) {
        eventoMentoradoService.cancelarAdmin(mentoradoId, eventoId);
    }
}
