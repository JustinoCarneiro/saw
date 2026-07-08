package com.sawhub.hub.consolidated.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Projeção crua da query agregada — o cálculo de %/status fica em Java, não em SQL. */
public record MentoradoConsolidadoRow(
        UUID id,
        String nome,
        String negocio,
        BigDecimal crescimentoFaturamentoPct,
        Integer ferramentasConcluidas,
        Integer ferramentasTotal,
        Long totalEncaminhamentos,
        Long encaminhamentosConcluidos,
        Long pesoTotal,
        Long pesoConcluido
) {
}
