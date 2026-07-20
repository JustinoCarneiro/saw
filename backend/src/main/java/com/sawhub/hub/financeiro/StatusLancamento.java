package com.sawhub.hub.financeiro;

/** Máquina de estado H14.1/H14.4 (CLAUDE.md): Previsto -&gt; Realizado (ou Parcial -&gt; Realizado),
 * desvio Previsto -&gt; Vencido. M26 (change request pós-MVP, merge com {@code ContaPagarReceber})
 * unificou os antigos {@code StatusConta} (PENDENTE/PARCIAL/PAGO/RECEBIDO/VENCIDO) e
 * {@code StatusLancamento} (PREVISTO/REALIZADO) num só: PAGO/RECEBIDO colapsam em REALIZADO (a
 * direção já vem de {@link TipoLancamento} RECEITA/DESPESA — ter status separado por direção era
 * redundante); PENDENTE virou PREVISTO (mesmo conceito). VENCIDO só é alcançável a partir de
 * PREVISTO (nunca de PARCIAL — mesmo comportamento de antes, ver {@link VencimentoScheduler}). */
public enum StatusLancamento {
    PREVISTO,
    PARCIAL,
    REALIZADO,
    VENCIDO
}
