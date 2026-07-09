package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.StatusTarefa;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusTarefaRequest(@NotNull StatusTarefa novoStatus) {
}
