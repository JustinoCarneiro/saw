package com.sawhub.hub.mentoria.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarResumoRequest(@NotBlank String resumo) {
}
