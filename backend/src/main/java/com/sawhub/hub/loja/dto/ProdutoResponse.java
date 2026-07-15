package com.sawhub.hub.loja.dto;

import com.sawhub.hub.loja.CategoriaProduto;
import com.sawhub.hub.loja.Produto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProdutoResponse(
        UUID id,
        String titulo,
        String descricao,
        CategoriaProduto categoria,
        BigDecimal preco,
        BigDecimal precoOriginal,
        BigDecimal avaliacaoMedia,
        boolean destaque,
        int vendas,
        String arquivoUrl,
        String imagemUrl,
        boolean publicado,
        boolean vendaEmAtacado,
        Instant criadoEm
) {
    public static ProdutoResponse from(Produto p) {
        return new ProdutoResponse(p.getId(), p.getTitulo(), p.getDescricao(), p.getCategoria(), p.getPreco(),
                p.getPrecoOriginal(), p.getAvaliacaoMedia(), p.isDestaque(), p.getVendas(), p.getArquivoUrl(),
                p.getImagemUrl(), p.isPublicado(), p.isVendaEmAtacado(), p.getCriadoEm());
    }
}
