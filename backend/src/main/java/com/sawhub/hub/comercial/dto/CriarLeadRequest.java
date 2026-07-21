package com.sawhub.hub.comercial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** H1.3 — corpo do formulário público "Solicitar acesso". Endpoint sem autenticação: os limites
 * de tamanho aqui não são só UX, evitam payload abusivo virar erro de banco (VARCHAR estourado)
 * ou vetor de armazenamento de lixo em massa. */
public record CriarLeadRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String telefone,
        @Size(max = 500) String mensagem
) {
}
