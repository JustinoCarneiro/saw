package com.sawhub.hub.comercial;

/** M25 — de onde veio a venda (planilha real "Origem da Venda"). DIRETA é quando o próprio
 * vendedor ({@link Lead#getVendedor()}) fechou; os demais são canais externos/cortesia. */
public enum OrigemVenda {
    DIRETA,
    HOTMART,
    CORTESIA,
    PATROCINIO,
    PALESTRANTE
}
