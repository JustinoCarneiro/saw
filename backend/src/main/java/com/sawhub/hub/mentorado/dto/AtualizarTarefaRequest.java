package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record AtualizarTarefaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotNull LocalDate prazo,
        Prioridade prioridade,
        UUID metaId
) {
}
