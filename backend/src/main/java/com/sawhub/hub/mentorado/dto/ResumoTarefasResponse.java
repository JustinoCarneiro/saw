package com.sawhub.hub.mentorado.dto;

/** H4.2/H4.3 — sempre calculado sobre TODAS as tarefas do mentorado, independente do filtro
 * selecionado na tela (mesmo padrão do resumo de Metas, M09). */
public record ResumoTarefasResponse(long total, long concluidas, long emAndamento, long pendentes) {
}
