package com.sawhub.hub.consolidated.dto;

import com.sawhub.hub.mentorado.NivelEngajamento;
import com.sawhub.hub.mentorado.ProgressoCalculator;
import com.sawhub.hub.mentorado.RiscoChurn;
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
        String status,
        Integer frequenciaMentoriaPct,
        NivelEngajamento nivelEngajamento,
        RiscoChurn riscoChurn
) {
    // E17/M27 — frequenciaMentoriaPct vem de fora (PresencaMentoriaRepository, join separado em
    // ConsolidatedService) porque MentoradoConsolidadoRow não carrega esse dado; null (não 0) se o
    // mentorado nunca participou de nenhuma mentoria em grupo realizada, ver ROADMAP.md §
    // "Blueprint (M27)". nivelEngajamento/riscoChurn não mudam o cálculo de `status` abaixo — o
    // EM_DIA/ATENCAO/ATRASADO continua exatamente como antes, os dois eixos são informação
    // adicional lida direto de Mentorado via a própria row.
    public static MentoradoConsolidadoResponse from(MentoradoConsolidadoRow row, Integer frequenciaMentoriaPct) {
        // M08 — mesma fórmula do Dashboard do Mentorado (ProgressoCalculator), pra não divergir.
        int progresso = ProgressoCalculator.pctPeso(row.pesoConcluido(), row.pesoTotal());
        int ferramentasPct = row.ferramentasTotal() == 0 ? 0
                : (int) Math.round(row.ferramentasConcluidas() * 100.0 / row.ferramentasTotal());
        String status = progresso >= 60 ? "EM_DIA" : progresso >= 30 ? "ATENCAO" : "ATRASADO";
        return new MentoradoConsolidadoResponse(
                row.id(), row.nome(), row.negocio(), progresso,
                row.encaminhamentosConcluidos(), row.totalEncaminhamentos(),
                ferramentasPct, row.crescimentoFaturamentoPct(), status,
                frequenciaMentoriaPct, row.nivelEngajamento(), row.riscoChurn());
    }
}
