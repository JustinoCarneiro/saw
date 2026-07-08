package com.sawhub.hub.team.dto;

import com.sawhub.hub.team.Area;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarColaboradorRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String senha,
        @NotNull Area area
) {
}
