package com.sawhub.hub.evento.dto;

import com.sawhub.hub.evento.StatusEvento;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusEventoRequest(@NotNull StatusEvento novoStatus) {
}
