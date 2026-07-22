package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

/** "Caixa do mês: Inicial, saldo por banco, Final" (reunião 17/07/2026) — {@code totalInicial}/
 * {@code totalFinal} somam todas as contas ativas com posição registrada no período. */
public record CaixaMensalResponse(
        int ano,
        int mes,
        List<PosicaoCaixaMensalResponse> contas,
        BigDecimal totalInicial,
        BigDecimal totalFinal
) {
}
