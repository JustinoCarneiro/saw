package com.sawhub.hub.financeiro;

/** Só relevante quando {@link TipoLancamento#RECEITA} — alimenta o dashboard de faturamento
 * (H14.3): composição da receita e MRR (só ASSINATURA entra no MRR). */
public enum OrigemReceita {
    ASSINATURA,
    LOJA,
    EVENTO,
    OUTRA
}
