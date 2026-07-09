package com.sawhub.hub.conteudo.dto;

import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarConteudoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotNull TipoConteudo tipo,
        // Mesmo achado/motivo do CriarConteudoRequest — ver comentário lá.
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+") String url,
        Plano planoMinimo
) {
}
