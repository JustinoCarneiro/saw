package com.sawhub.hub.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String novaSenha
) {
}
