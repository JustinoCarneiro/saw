package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;

public record DreResponse(
        String periodo,
        BigDecimal receitaBruta,
        BigDecimal deducoes,
        BigDecimal receitaLiquida,
        BigDecimal custos,
        BigDecimal despesasOperacionais,
        BigDecimal resultado,
        ComparativoMes comparativoMesAnterior
) {
}
