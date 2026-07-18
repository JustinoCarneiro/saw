package com.sawhub.hub.comercial.dto;

import java.math.BigDecimal;
import java.util.UUID;

// M25 (Suposição 7) — venda de ingresso por evento REALIZADO no período do dashboard comercial,
// à parte do "vendido no mês" (que exclui INGRESSO_EVENTO).
public record VendaIngressoResumo(
        UUID eventoId,
        String eventoTitulo,
        long quantidadeVendida,
        Integer quantidadeTotal,
        BigDecimal valorLiquido
) {
}
