package com.sawhub.hub.comercial.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardComercialResponse(
        long novosMentoradosNoMes,
        double taxaConversaoPct,
        BigDecimal mrr,
        BigDecimal vendasLoja,
        double variacaoMrrPct,
        List<FunilItem> funil,
        // M25 (Suposição 7) — "vendido no mês" (novosMentoradosNoMes) exclui INGRESSO_EVENTO;
        // essa seção mostra a venda de ingresso à parte, por evento REALIZADO no período.
        List<VendaIngressoResumo> vendaIngressos,
        // Pedido do Marcos (22/07/2026) — segunda metade de "dashboard mais visual: venda de
        // ingresso por evento + venda 'por fora' (produto + valor)" que faltava (reunião
        // 17/07/2026). Exclui INGRESSO_EVENTO (já coberto por vendaIngressos acima).
        List<VendaPorProdutoResumo> vendaPorFora
) {
}
