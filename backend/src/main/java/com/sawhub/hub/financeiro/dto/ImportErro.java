package com.sawhub.hub.financeiro.dto;

/** M21 — "linha" conta o cabeçalho como linha 1 (mesma numeração que o usuário vê ao abrir o
 * arquivo num editor de planilhas). */
public record ImportErro(int linha, String motivo) {
}
