package com.sawhub.hub.comercial;

/** M25 — forma de pagamento da venda (planilha real "Vendas Aline Melo": Hotmart, Pix Recorrente,
 * Pix...). Genérico o bastante pra cobrir venda direta e via Hotmart. */
public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO,
    HOTMART
}
