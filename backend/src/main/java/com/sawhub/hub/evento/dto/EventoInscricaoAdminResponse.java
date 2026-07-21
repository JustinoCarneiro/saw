package com.sawhub.hub.evento.dto;

import com.sawhub.hub.evento.InscricaoEvento;
import com.sawhub.hub.evento.StatusInscricao;
import com.sawhub.hub.evento.TipoEvento;
import java.time.Instant;
import java.util.UUID;

/** M28 (change request, 21/07/2026) — histórico de inscrições de um mentorado em eventos, pra
 * tela admin (MentoradoDetalhePage) mostrar o que já foi usado da cota de 3 grátis/ano (Mentoria
 * Contínua) e permitir cancelar. */
public record EventoInscricaoAdminResponse(
        UUID eventoId,
        String titulo,
        TipoEvento tipo,
        Instant dataHora,
        StatusInscricao status
) {
    public static EventoInscricaoAdminResponse from(InscricaoEvento i) {
        var evento = i.getEvento();
        return new EventoInscricaoAdminResponse(evento.getId(), evento.getTitulo(), evento.getTipo(),
                evento.getDataHora(), i.getStatus());
    }
}
