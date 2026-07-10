package com.sawhub.hub.perfil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** H9.1 — autoedição do mentorado. Só contato/preferências: nome/negócio/plano continuam
 * admin-only (ver Suposição 1 do Blueprint M15 e {@code AtualizarMentoradoRequest}). */
public record AtualizarPerfilMentoradoRequest(
        @Size(max = 30) String telefone,
        @Size(max = 500) String bio,
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> areasInteresse,
        @Size(max = 500) @Pattern(regexp = "^https?://.+") String fotoUrl
) {
}
