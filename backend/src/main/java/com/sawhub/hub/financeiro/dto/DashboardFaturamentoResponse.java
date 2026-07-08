package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardFaturamentoResponse(
        BigDecimal faturamentoMensal,
        BigDecimal mrr,
        double churnPct,
        List<ComposicaoReceita> composicao
) {
}
