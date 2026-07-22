package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.ProdutoVenda;
import java.math.BigDecimal;

/** Pedido do Marcos (22/07/2026, achado na auditoria de clareza) — a segunda metade do pedido
 * original "dashboard comercial mais visual (...) venda 'por fora' (produto + valor)" (reunião
 * 17/07/2026) nunca tinha sido construída; só a venda de ingresso por evento existia. Exclui
 * {@link ProdutoVenda#INGRESSO_EVENTO} (tem seção própria, {@link VendaIngressoResumo}). */
public record VendaPorProdutoResumo(ProdutoVenda produto, long quantidade, BigDecimal valorTotal) {
}
