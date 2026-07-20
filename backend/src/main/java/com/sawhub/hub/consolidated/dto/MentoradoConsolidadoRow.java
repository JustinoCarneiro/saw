package com.sawhub.hub.consolidated.dto;

import com.sawhub.hub.mentorado.NivelEngajamento;
import com.sawhub.hub.mentorado.RiscoChurn;
import java.math.BigDecimal;
import java.util.UUID;

/** Projeção crua da query agregada — o cálculo de %/status fica em Java, não em SQL.
 * nivelEngajamento/riscoChurn (E17/M27) são lidos direto de Mentorado, sem cálculo — ver
 * ROADMAP.md § "Blueprint (M27)". */
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
        Long pesoConcluido,
        NivelEngajamento nivelEngajamento,
        RiscoChurn riscoChurn
) {
}
