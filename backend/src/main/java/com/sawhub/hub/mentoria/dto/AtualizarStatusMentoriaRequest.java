package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.mentoria.StatusMentoria;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusMentoriaRequest(@NotNull StatusMentoria novoStatus) {
}
