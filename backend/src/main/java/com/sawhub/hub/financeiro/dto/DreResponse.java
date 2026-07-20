package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;

// E14 — despesasFixas/despesasVariaveis (raio-x da planilha real, achado: Fixa/Variável é
// consistente por subcategoria, ver CategoriaFinanceira.natureza). Só soma categorias com
// natureza preenchida — categorias sem natureza (ex. ligadas a evento) não entram em nenhuma das
// duas, mas continuam contando em despesasOperacionais/custos normalmente.
public record DreResponse(
        String periodo,
        BigDecimal receitaBruta,
        BigDecimal deducoes,
        BigDecimal receitaLiquida,
        BigDecimal custos,
        BigDecimal despesasOperacionais,
        BigDecimal despesasFixas,
        BigDecimal despesasVariaveis,
        BigDecimal resultado,
        ComparativoMes comparativoMesAnterior
) {
}
