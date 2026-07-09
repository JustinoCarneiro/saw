package com.sawhub.hub.meta.dto;

/** H3.3 — sempre calculado sobre TODAS as metas do mentorado, independente do filtro selecionado
 * na tela (ver ROADMAP.md M09). */
public record ResumoMetasResponse(int conclusaoMediaPct, long concluidas, long noPrazo, long atrasadas) {
}
