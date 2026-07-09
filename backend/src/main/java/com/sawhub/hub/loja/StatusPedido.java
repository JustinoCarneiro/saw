package com.sawhub.hub.loja;

/** CLAUDE.md § Máquinas de estado: "Pedido (Loja): Carrinho -> Aguardando pagamento -> Pago ->
 * Liberado, desvios: Cancelado, Reembolsado". PAGO e LIBERADO transitam juntos, automaticamente,
 * no mesmo webhook (catálogo 100% digital nesta leva — ver Suposições do Blueprint M14). */
public enum StatusPedido {
    CARRINHO,
    AGUARDANDO_PAGAMENTO,
    PAGO,
    LIBERADO,
    CANCELADO,
    REEMBOLSADO
}
