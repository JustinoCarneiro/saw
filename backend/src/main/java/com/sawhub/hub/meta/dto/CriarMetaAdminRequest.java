package com.sawhub.hub.meta.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

// Diferente de CriarMetaRequest (self-service, M09, mentorado resolvido do usuário autenticado):
// Admin cria pra qualquer mentorado, então mentoradoId vem no corpo da requisição.
public record CriarMetaAdminRequest(
        @NotNull UUID mentoradoId,
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 1000) String descricao,
        @NotNull @FutureOrPresent LocalDate prazo
) {
}
