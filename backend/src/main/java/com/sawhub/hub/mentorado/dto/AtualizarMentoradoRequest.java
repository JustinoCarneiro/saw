package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarMentoradoRequest(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 255) String negocio,
        @NotNull Plano plano
) {
}
