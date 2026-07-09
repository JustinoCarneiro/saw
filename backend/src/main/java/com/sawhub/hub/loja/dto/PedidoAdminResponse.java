package com.sawhub.hub.loja.dto;

import com.sawhub.hub.loja.ItemPedido;
import com.sawhub.hub.loja.Pedido;
import com.sawhub.hub.loja.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoAdminResponse(
        UUID id,
        UUID mentoradoId,
        String mentoradoNome,
        StatusPedido status,
        BigDecimal valorTotal,
        String referenciaGateway,
        Instant criadoEm,
        List<ItemPedidoAdminResponse> itens
) {
    public record ItemPedidoAdminResponse(String titulo, int quantidade, BigDecimal precoUnitario) {
        public static ItemPedidoAdminResponse from(ItemPedido i) {
            return new ItemPedidoAdminResponse(i.getProduto().getTitulo(), i.getQuantidade(), i.getPrecoUnitario());
        }
    }

    public static PedidoAdminResponse from(Pedido p) {
        return new PedidoAdminResponse(p.getId(), p.getMentorado().getId(), p.getMentorado().getNome(), p.getStatus(),
                p.getValorTotal(), p.getReferenciaGateway(), p.getCriadoEm(),
                p.getItens().stream().map(ItemPedidoAdminResponse::from).toList());
    }
}
