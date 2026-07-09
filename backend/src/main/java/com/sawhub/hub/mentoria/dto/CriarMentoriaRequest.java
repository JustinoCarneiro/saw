package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.mentoria.TipoMentoria;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        // Achado (baixo) do revisor-seguranca no M12: mesmo esquema exigido de Conteudo.url desde
        // o M11 — o M12 é a primeira vez que este campo vira alvo recorrente de botão "Entrar na
        // reunião"/.ics/Google Calendar pro mentorado (antes só um link discreto no dashboard do
        // M08), então a superfície de uso justifica trazer pro mesmo padrão agora. @Pattern só
        // valida quando presente (campo continua opcional — mentoria pode ser só presencial).
        @Pattern(regexp = "^https?://.+") String linkOnline,
        String local
) {
}
