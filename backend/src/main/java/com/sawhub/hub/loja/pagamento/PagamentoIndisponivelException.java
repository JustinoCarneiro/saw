package com.sawhub.hub.loja.pagamento;

/** Lançada quando o gateway de pagamento (Mercado Pago) não está configurado ou falha — mesmo
 * papel do IaIndisponivelException (M06) pro pipeline de IA: falha limpa e explícita em vez de
 * uma chamada HTTP fadada a dar 401, ou pior, um checkout que parece ter funcionado sem ter. */
public class PagamentoIndisponivelException extends RuntimeException {
    public PagamentoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }

    public PagamentoIndisponivelException(String message) {
        super(message);
    }
}
