package com.sawhub.hub.comercial;

/** M25 — forma de pagamento da venda (planilha real "Vendas Aline Melo": Hotmart, Pix Recorrente,
 * Pix...). Genérico o bastante pra cobrir venda direta e via Hotmart.
 *
 * <p>{@link #PIX_RECORRENTE} (gap 6, confirmado 19/07/2026): dado real distingue Pix avulso de
 * Pix recorrente (assinatura) — pode importar pro cálculo de MRR (H14.3) mais adiante. */
public enum FormaPagamento {
    PIX,
    PIX_RECORRENTE,
    CARTAO,
    BOLETO,
    HOTMART
}
