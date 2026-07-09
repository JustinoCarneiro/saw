package com.sawhub.hub.evento.dto;

import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.evento.TipoEvento;
import java.time.Instant;
import java.util.UUID;

/** H7.1-H7.2 (M13) — mentee-facing, deliberadamente diferente de {@code EventoResponse} (Admin):
 * expõe {@code vagasDisponiveis} (derivado, nunca a contagem crua de ocupação) e {@code inscrito}
 * (estado do mentorado atual, não existe no lado Admin). */
public record EventoMentoradoResponse(
        UUID id,
        String titulo,
        TipoEvento tipo,
        String tema,
        Instant dataHora,
        String local,
        String linkOnline,
        Integer vagas,
        Integer vagasDisponiveis,
        StatusEvento status,
        boolean inscrito
) {
    public static EventoMentoradoResponse from(Evento e, boolean inscrito) {
        return new EventoMentoradoResponse(e.getId(), e.getTitulo(), e.getTipo(), e.getTema(), e.getDataHora(),
                e.getLocal(), e.getLinkOnline(), e.getVagas(), e.getVagasDisponiveis(), e.getStatus(), inscrito);
    }
}
