package com.sawhub.hub.meta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AtualizarMetaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 1000) String descricao,
        @NotNull LocalDate prazo,
        @NotNull @Min(0) @Max(100) Integer progressoPct
) {
}
