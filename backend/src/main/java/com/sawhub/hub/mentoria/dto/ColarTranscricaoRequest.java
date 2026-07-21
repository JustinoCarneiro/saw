package com.sawhub.hub.mentoria.dto;

import jakarta.validation.constraints.NotBlank;

/** M28 (change request, 21/07/2026) — "colar transcrição do Google Meet" em vez de subir áudio
 * (aditivo, ver AtaService#iniciarComTranscricaoColada). */
public record ColarTranscricaoRequest(@NotBlank String transcricao) {
}
