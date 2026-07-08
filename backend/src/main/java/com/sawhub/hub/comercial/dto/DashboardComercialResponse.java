package com.sawhub.hub.comercial.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardComercialResponse(
        long novosMentoradosNoMes,
        double taxaConversaoPct,
        BigDecimal mrr,
        BigDecimal vendasLoja,
        double variacaoMrrPct,
        List<FunilItem> funil
) {
}
