package com.sawhub.hub.loja.dto;

import com.sawhub.hub.loja.CategoriaProduto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CriarProdutoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotBlank String descricao,
        @NotNull CategoriaProduto categoria,
        @NotNull @Positive BigDecimal preco,
        @PositiveOrZero BigDecimal precoOriginal,
        @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal avaliacaoMedia,
        boolean destaque,
        // Mesmo achado do M11 (Conteudo.url)/M12 (Mentoria.linkOnline)/M13 (Evento.linkOnline):
        // campo vira link clicável/download direto pro mentorado quando o pedido é liberado.
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+") String arquivoUrl,
        @Size(max = 500) @Pattern(regexp = "^https?://.+") String imagemUrl,
        boolean vendaEmAtacado
) {
}
