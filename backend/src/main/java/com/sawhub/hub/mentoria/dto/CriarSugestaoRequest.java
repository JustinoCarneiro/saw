package com.sawhub.hub.mentoria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Auditoria de UX (22/07/2026) — encaminhamento digitado manualmente pelo mentor (ver
// AtaEncaminhamentoSugerido), não mais sugerido pela IA.
public record CriarSugestaoRequest(
        @NotBlank String titulo,
        @NotNull @Min(1) @Max(2) Integer pesoSugerido
) {
}
