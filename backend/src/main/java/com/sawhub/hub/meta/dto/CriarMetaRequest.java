package com.sawhub.hub.meta.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CriarMetaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 1000) String descricao,
        @NotNull @FutureOrPresent LocalDate prazo
) {
}
