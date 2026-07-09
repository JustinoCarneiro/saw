package com.sawhub.hub.mentoria.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// Lista vazia é válida de propósito (limpa os materiais recomendados) — por isso @NotNull,
// não @NotEmpty. Ver PATCH /admin/mentorias/{id}/materiais no ROADMAP.md M12.
public record AtualizarMateriaisMentoriaRequest(@NotNull List<UUID> conteudoIds) {
}
