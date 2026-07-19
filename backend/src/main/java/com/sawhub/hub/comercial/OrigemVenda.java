package com.sawhub.hub.comercial;

/** M25 — de onde veio a venda (planilha real "Origem da Venda"). DIRETA é quando o próprio
 * vendedor ({@link Lead#getVendedor()}) fechou; os demais são canais externos/cortesia.
 *
 * <p>{@link #PARCEIRO} (confirmado 19/07/2026, ver docs/reuniao-2026-07-17-atualizacoes.md §
 * "Perguntas pendentes pro Victor"): categoria própria, diferente de {@link #CORTESIA} — venda
 * direta comercial já é coberta por DIRETA + {@link Lead#getVendedor()} (nome da vendedora), não
 * precisa de valor de enum próprio pra isso. */
public enum OrigemVenda {
    DIRETA,
    HOTMART,
    CORTESIA,
    PATROCINIO,
    PALESTRANTE,
    PARCEIRO
}
