package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.EstadoImplementacao;
import jakarta.validation.constraints.NotNull;

/** E17/M27 — as 4 ferramentas obrigatórias nomeadas do ranking (ver ROADMAP.md § "Blueprint
 * (M27)"). Todas obrigatórias na request — a tela sempre mostra as 4 juntas, mesmo padrão de
 * {@code AtualizarDiagnosticoInicialRequest}. */
public record AtualizarFerramentasObrigatoriasRequest(
        @NotNull EstadoImplementacao ferramentaDre,
        @NotNull EstadoImplementacao ferramentaManualCultura,
        @NotNull EstadoImplementacao ferramentaFichaTecnica,
        @NotNull EstadoImplementacao ferramentaManualProcessos
) {
}
