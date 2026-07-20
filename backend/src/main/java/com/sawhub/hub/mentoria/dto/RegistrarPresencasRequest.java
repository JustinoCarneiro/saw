package com.sawhub.hub.mentoria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** E17/M27 — presença por mentorado numa mentoria GRUPO (ver Mentoria#getTipo/
 * MentoriaService#registrarPresencas). */
public record RegistrarPresencasRequest(@NotEmpty @Valid List<PresencaRequest> presencas) {

    public record PresencaRequest(@NotNull UUID mentoradoId, boolean presente) {
    }
}
