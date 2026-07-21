package com.sawhub.hub.mentorado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// telefone/bio/fotoUrl (Fase 5, H11.1): mesmos limites/validação de
// AtualizarPerfilMentoradoRequest (autoedição, H9.1) — Admin e o próprio mentorado escrevem nos
// mesmos campos, então as regras precisam ser as mesmas dos dois lados.
public record AtualizarMentoradoRequest(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 255) String negocio,
        @Size(max = 30) String telefone,
        @Size(max = 500) String bio,
        @Size(max = 500) @Pattern(regexp = "^https?://.+") String fotoUrl
) {
}
