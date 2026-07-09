package com.sawhub.hub.consolidated.dto;

import com.sawhub.hub.mentorado.ProgressoCalculator;
import java.math.BigDecimal;
import java.util.UUID;

public record MentoradoConsolidadoResponse(
        UUID id,
        String nome,
        String negocio,
        int progressoPct,
        long encaminhamentosCumpridos,
        long encaminhamentosTotal,
        int ferramentasPct,
        BigDecimal crescimentoFaturamentoPct,
        String status
) {
    public static MentoradoConsolidadoResponse from(MentoradoConsolidadoRow row) {
        // M08 — mesma fórmula do Dashboard do Mentorado (ProgressoCalculator), pra não divergir.
        int progresso = ProgressoCalculator.pctPeso(row.pesoConcluido(), row.pesoTotal());
        int ferramentasPct = row.ferramentasTotal() == 0 ? 0
                : (int) Math.round(row.ferramentasConcluidas() * 100.0 / row.ferramentasTotal());
        String status = progresso >= 60 ? "EM_DIA" : progresso >= 30 ? "ATENCAO" : "ATRASADO";
        return new MentoradoConsolidadoResponse(
                row.id(), row.nome(), row.negocio(), progresso,
                row.encaminhamentosConcluidos(), row.totalEncaminhamentos(),
                ferramentasPct, row.crescimentoFaturamentoPct(), status);
    }
}
