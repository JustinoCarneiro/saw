package com.sawhub.hub.mentoria.dto;

import java.util.UUID;

/** E17/M27 — projeção crua da query agregada de frequência em mentoria coletiva; o cálculo de %
 * fica em Java (mesmo padrão de {@code MentoradoConsolidadoRow}, consolidated/). */
public record FrequenciaMentoriaRow(UUID mentoradoId, Long totalMentoriasGrupo, Long presencasConfirmadas) {
}
