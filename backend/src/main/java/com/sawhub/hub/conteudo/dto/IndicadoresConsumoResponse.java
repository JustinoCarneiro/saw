package com.sawhub.hub.conteudo.dto;

/** H6.3 — "consumo conta nos meus indicadores (dias assistidos, minutos, favoritas)"
 * (docs/spec.md). {@code minutosAssistidos} soma a duração DECLARADA (Conteudo.duracaoMinutos,
 * preenchida pelo Admin) dos conteúdos assistidos — não é tempo real de exibição, o projeto não
 * rastreia player; conteúdos sem duração cadastrada não entram na soma. */
public record IndicadoresConsumoResponse(int diasAssistidos, long favoritas, int minutosAssistidos) {
}
