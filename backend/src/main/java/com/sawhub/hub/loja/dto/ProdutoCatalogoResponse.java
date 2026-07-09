package com.sawhub.hub.loja.dto;

import com.sawhub.hub.loja.CategoriaProduto;
import com.sawhub.hub.loja.Produto;
import java.math.BigDecimal;
import java.util.UUID;

/** H8.1 (M14) — mentee-facing, deliberadamente sem `arquivoUrl` (só liberado após pagamento, ver
 * {@link PedidoMentoradoResponse}). */
public record ProdutoCatalogoResponse(
        UUID id,
        String titulo,
        String descricao,
        CategoriaProduto categoria,
        BigDecimal preco,
        BigDecimal precoOriginal,
        BigDecimal avaliacaoMedia,
        boolean destaque,
        int vendas,
        String imagemUrl
) {
    public static ProdutoCatalogoResponse from(Produto p) {
        return new ProdutoCatalogoResponse(p.getId(), p.getTitulo(), p.getDescricao(), p.getCategoria(), p.getPreco(),
                p.getPrecoOriginal(), p.getAvaliacaoMedia(), p.isDestaque(), p.getVendas(), p.getImagemUrl());
    }
}
