package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;

/** E14 — "mais gráficos e detalhe que estão nas planilhas do financeiro" (reunião 17/07/2026):
 * quebra de Receita/Despesa por {@link com.sawhub.hub.financeiro.CategoriaFinanceira#getNome()},
 * mesma granularidade da planilha real "DRE Financeira Saw". Deliberadamente por nome de
 * categoria, não por {@code grupo} — este último é texto livre que o Admin ainda não populou em
 * massa (CLAUDE.md: "não é classificação pra inventar"), então agrupar por ele hoje devolveria
 * tudo em um único balde "sem grupo". */
public record CategoriaValor(String categoria, BigDecimal valor) {
}
