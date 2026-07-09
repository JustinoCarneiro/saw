package com.sawhub.hub.mentoria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarSugestaoRequest(
        @NotBlank String titulo,
        @NotNull @Min(1) @Max(2) Integer pesoSugerido,
        boolean aceito
) {
}
