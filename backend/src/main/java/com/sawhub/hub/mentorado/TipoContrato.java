package com.sawhub.hub.mentorado;

import java.time.LocalDate;

/** Change request pós-MVP (reunião 17/07/2026, docs/reuniao-2026-07-17-atualizacoes.md) —
 * "não existem planos, mas sim produtos". Conceito NOVO e aditivo, só pro lado comercial
 * (Mentorado/Lead — "o que foi vendido"); {@link Plano} continua existindo e gateando conteúdo
 * exatamente como hoje (ver Suposição 1 do Blueprint M23 em ROADMAP.md — cisão, não substituição). */
public enum TipoContrato {
    MENTORIA_CONTINUA,
    MENTORIA_INDIVIDUAL,
    CONSULTORIA;

    /** 12 meses fixos da data de fechamento pra Contínua/Individual. Consultoria é "esporádica"
     * (confirmado com o cliente) — sem prazo fixo, retorna null de propósito, não uma data
     * fabricada. */
    public LocalDate calcularVencimento(LocalDate dataFechamentoContrato) {
        return switch (this) {
            case MENTORIA_CONTINUA, MENTORIA_INDIVIDUAL -> dataFechamentoContrato.plusMonths(12);
            case CONSULTORIA -> null;
        };
    }
}
