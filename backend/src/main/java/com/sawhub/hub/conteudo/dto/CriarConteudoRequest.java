package com.sawhub.hub.conteudo.dto;

import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarConteudoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotNull TipoConteudo tipo,
        @NotBlank @Size(max = 500) String url,
        Plano planoMinimo
) {
}
