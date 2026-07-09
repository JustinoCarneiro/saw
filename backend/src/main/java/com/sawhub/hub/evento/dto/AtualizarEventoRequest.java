package com.sawhub.hub.evento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AtualizarEventoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @Size(max = 255) String tema,
        @NotNull Instant dataHora,
        @Size(max = 255) String local,
        @Size(max = 500) String linkOnline,
        @Positive Integer vagas
) {
}
