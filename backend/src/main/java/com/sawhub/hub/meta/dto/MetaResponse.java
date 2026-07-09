package com.sawhub.hub.meta.dto;

import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.meta.StatusMeta;
import com.sawhub.hub.meta.SubStatusMeta;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record MetaResponse(
        UUID id,
        String titulo,
        String descricao,
        LocalDate prazo,
        long diasRestantes,
        Integer progressoPct,
        StatusMeta status,
        SubStatusMeta subStatus
) {
    // Suposição a validar com o cliente (ROADMAP.md M09) — limiar de dias pra "Atenção".
    private static final long LIMITE_ATENCAO_DIAS = 7;

    public static MetaResponse from(Meta meta, LocalDate hoje) {
        long diasRestantes = ChronoUnit.DAYS.between(hoje, meta.getPrazo());
        SubStatusMeta subStatus = subStatus(meta.getStatus(), diasRestantes);
        return new MetaResponse(meta.getId(), meta.getTitulo(), meta.getDescricao(), meta.getPrazo(),
                diasRestantes, meta.getProgressoPct(), meta.getStatus(), subStatus);
    }

    private static SubStatusMeta subStatus(StatusMeta status, long diasRestantes) {
        if (status != StatusMeta.ATIVA) {
            return null;
        }
        if (diasRestantes < 0) {
            return SubStatusMeta.ATRASADA;
        }
        return diasRestantes <= LIMITE_ATENCAO_DIAS ? SubStatusMeta.ATENCAO : SubStatusMeta.NO_PRAZO;
    }
}
