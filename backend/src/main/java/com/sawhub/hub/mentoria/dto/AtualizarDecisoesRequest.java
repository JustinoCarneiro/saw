package com.sawhub.hub.mentoria.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarDecisoesRequest(@NotBlank String decisoes) {
}
