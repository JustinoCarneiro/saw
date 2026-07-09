package com.sawhub.hub.loja.dto;

import com.sawhub.hub.loja.ItemPedido;
import com.sawhub.hub.loja.Pedido;
import com.sawhub.hub.loja.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** H8.3-H8.4 (M14) — histórico do mentorado. `arquivoUrl` só aparece quando `status == LIBERADO`
 * — mesmo raciocínio do "ata RASCUNHO nunca aparece" do M12: dado sensível ao estado do PEDIDO,
 * não uma checagem de frontend (o mentorado não pode baixar o item antes do pagamento liberar). */
public record PedidoMentoradoResponse(
        UUID id,
        StatusPedido status,
        BigDecimal valorTotal,
        Instant criadoEm,
        List<ItemPedidoMentoradoResponse> itens
) {
    public record ItemPedidoMentoradoResponse(String titulo, int quantidade, BigDecimal precoUnitario, String arquivoUrl) {
        public static ItemPedidoMentoradoResponse from(ItemPedido item, boolean liberado) {
            return new ItemPedidoMentoradoResponse(item.getProduto().getTitulo(), item.getQuantidade(),
                    item.getPrecoUnitario(), liberado ? item.getProduto().getArquivoUrl() : null);
        }
    }

    public static PedidoMentoradoResponse from(Pedido p) {
        boolean liberado = p.getStatus() == StatusPedido.LIBERADO;
        List<ItemPedidoMentoradoResponse> itens = p.getItens().stream()
                .map(i -> ItemPedidoMentoradoResponse.from(i, liberado))
                .toList();
        return new PedidoMentoradoResponse(p.getId(), p.getStatus(), p.getValorTotal(), p.getCriadoEm(), itens);
    }
}
