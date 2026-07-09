package com.sawhub.hub.meta.dto;

import com.sawhub.hub.meta.StatusMeta;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusMetaRequest(@NotNull StatusMeta novoStatus) {
}
