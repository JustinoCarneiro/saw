package com.sawhub.hub.aviso.dto;

import com.sawhub.hub.aviso.CategoriaAviso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarAvisoRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 1000) String descricao,
        @NotNull CategoriaAviso categoria
) {
}
