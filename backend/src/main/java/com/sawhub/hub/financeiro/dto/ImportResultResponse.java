package com.sawhub.hub.financeiro.dto;

import java.util.List;

/** M21 — import tudo-ou-nada: se {@code erros} não está vazio, nada foi persistido
 * ({@code importados} é sempre 0 nesse caso) — ver Blueprint (ROADMAP.md) pra justificativa
 * (LancamentoFinanceiro é imutável, sem endpoint de exclusão pra desfazer um import parcial). */
public record ImportResultResponse(int totalLinhas, int importados, List<ImportErro> erros) {
}
