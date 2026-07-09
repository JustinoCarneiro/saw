package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.mentoria.TipoMentoria;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CriarMentoriaRequest(
        @NotNull TipoMentoria tipo,
        @NotEmpty List<UUID> mentoradoIds,
        @NotNull UUID mentorId,
        @NotNull Instant dataHora,
        @NotNull @Positive Integer duracaoMin,
        String linkOnline,
        String local
) {
}
