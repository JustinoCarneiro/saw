package com.sawhub.hub.loja.dto;

import com.sawhub.hub.loja.ItemPedido;
import com.sawhub.hub.loja.Pedido;
import com.sawhub.hub.loja.StatusPedido;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CarrinhoResponse(
        UUID id,
        StatusPedido status,
        BigDecimal valorTotal,
        List<ItemCarrinhoResponse> itens
) {
    public record ItemCarrinhoResponse(
            UUID id,
            UUID produtoId,
            String titulo,
            String imagemUrl,
            int quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
        public static ItemCarrinhoResponse from(ItemPedido i) {
            return new ItemCarrinhoResponse(i.getId(), i.getProduto().getId(), i.getProduto().getTitulo(),
                    i.getProduto().getImagemUrl(), i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal());
        }
    }

    public static CarrinhoResponse from(Pedido p) {
        return new CarrinhoResponse(p.getId(), p.getStatus(), p.getValorTotal(),
                p.getItens().stream().map(ItemCarrinhoResponse::from).toList());
    }

    // Carrinho vazio não é um Pedido real — não cria linha no banco até o 1º item ser
    // adicionado (ver LojaMentoradoService.adicionarItem e Suposições do Blueprint M14).
    public static CarrinhoResponse vazio() {
        return new CarrinhoResponse(null, StatusPedido.CARRINHO, BigDecimal.ZERO, List.of());
    }
}
