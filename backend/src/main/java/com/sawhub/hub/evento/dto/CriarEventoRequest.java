package com.sawhub.hub.evento.dto;

import com.sawhub.hub.evento.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CriarEventoRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotNull TipoEvento tipo,
        @Size(max = 255) String tema,
        @NotNull Instant dataHora,
        @Size(max = 255) String local,
        // Aplicado proativamente (lição do M12, achado 2): mesmo esquema de Conteudo.url/
        // Mentoria.linkOnline — este módulo (M13) é a primeira vez que o link de um evento vira
        // clicável pro mentorado. @Pattern só valida quando presente, campo continua opcional.
        @Size(max = 500) @Pattern(regexp = "^https?://.+") String linkOnline,
        @Positive Integer vagas
) {
}
