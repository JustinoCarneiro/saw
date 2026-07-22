package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

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
        ComparativoMes comparativoMesAnterior,
        // "mais gráficos e detalhe" (reunião 17/07/2026) — quebra por categoria real, pronta pro
        // gráfico de barras/pizza do frontend (ver CategoriaValor).
        List<CategoriaValor> receitaPorCategoria,
        List<CategoriaValor> despesaPorCategoria
) {
}
