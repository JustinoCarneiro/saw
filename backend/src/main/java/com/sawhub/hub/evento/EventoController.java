package com.sawhub.hub.evento;

import com.sawhub.hub.evento.dto.AtualizarEventoRequest;
import com.sawhub.hub.evento.dto.AtualizarStatusEventoRequest;
import com.sawhub.hub.evento.dto.CriarEventoRequest;
import com.sawhub.hub.evento.dto.EventoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H11.4 — CRUD admin de eventos. Nota do Blueprint (M06): `Modulo.CONTEUDOS` é um default
 * assumido (CLAUDE.md não define qual área do Time administra Eventos) — ajustar se a SAW
 * quiser separar num módulo próprio. */
@RestController
@RequestMapping("/api/v1/admin/eventos")
@RequiresModulo(Modulo.CONTEUDOS)
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse criar(@Valid @RequestBody CriarEventoRequest request) {
        return EventoResponse.from(eventoService.criar(request));
    }

    @GetMapping
    public List<EventoResponse> listar(@RequestParam(required = false) TipoEvento tipo,
                                        @RequestParam(required = false) StatusEvento status) {
        return eventoService.listar(tipo, status).stream().map(EventoResponse::from).toList();
    }

    @PutMapping("/{id}")
    public EventoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarEventoRequest request) {
        return EventoResponse.from(eventoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public EventoResponse atualizarStatus(@PathVariable UUID id, @Valid @RequestBody AtualizarStatusEventoRequest request) {
        return EventoResponse.from(eventoService.avancarStatus(id, request.novoStatus()));
    }
}
