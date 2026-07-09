package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Prioridade;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

// Sem campo de peso de propósito — self-service nasce sempre com peso 1 (ver ROADMAP.md M10,
// achado de integridade: mentorado não pode se auto-atribuir peso 2 do ranking do E17).
public record CriarTarefaRequest(
        @NotBlank @Size(max = 255) String titulo,
        @NotNull @FutureOrPresent LocalDate prazo,
        Prioridade prioridade,
        UUID metaId
) {
}
