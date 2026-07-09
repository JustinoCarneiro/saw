package com.sawhub.hub.evento.dto;

import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.evento.TipoEvento;
import java.time.Instant;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String titulo,
        TipoEvento tipo,
        String tema,
        Instant dataHora,
        String local,
        String linkOnline,
        Integer vagas,
        StatusEvento status
) {
    public static EventoResponse from(Evento e) {
        return new EventoResponse(e.getId(), e.getTitulo(), e.getTipo(), e.getTema(), e.getDataHora(),
                e.getLocal(), e.getLinkOnline(), e.getVagas(), e.getStatus());
    }
}
