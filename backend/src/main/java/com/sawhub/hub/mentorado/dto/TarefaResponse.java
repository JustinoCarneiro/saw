package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.Prioridade;
import com.sawhub.hub.mentorado.StatusTarefa;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record TarefaResponse(
        UUID id,
        String titulo,
        MetaResumo metaRelacionada,
        LocalDate prazo,
        Long diasRestantes,
        Prioridade prioridade,
        StatusTarefa status,
        boolean atrasada,
        Integer peso
) {
    public record MetaResumo(UUID id, String titulo) {
    }

    public static TarefaResponse from(Encaminhamento e, LocalDate hoje) {
        Long diasRestantes = e.getPrazo() == null ? null : ChronoUnit.DAYS.between(hoje, e.getPrazo());
        // H4.4 — sobrepõe visualmente PENDENTE/EM_ANDAMENTO sem criar um 4º valor de enum
        // persistido (mesmo padrão do sub-status de Meta, M09).
        boolean atrasada = e.getStatus() != StatusTarefa.CONCLUIDA && diasRestantes != null && diasRestantes < 0;
        MetaResumo meta = e.getMeta() == null ? null : new MetaResumo(e.getMeta().getId(), e.getMeta().getTitulo());
        return new TarefaResponse(e.getId(), e.getTitulo(), meta, e.getPrazo(), diasRestantes,
                e.getPrioridade(), e.getStatus(), atrasada, e.getPeso());
    }
}
