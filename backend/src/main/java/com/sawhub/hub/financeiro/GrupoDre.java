package com.sawhub.hub.financeiro;

/** Bucket de agregação do DRE (H14.2): Receita Bruta - Deduções = Líquida; Líquida - Custos -
 * Despesas Operacionais = Resultado. */
public enum GrupoDre {
    RECEITA_BRUTA,
    DEDUCOES,
    CUSTOS,
    DESPESA_OPERACIONAL
}
