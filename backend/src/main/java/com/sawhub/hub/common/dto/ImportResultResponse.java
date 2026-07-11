package com.sawhub.hub.common.dto;

import java.util.List;

/** M21/M22 — import tudo-ou-nada em todo módulo que usa: se {@code erros} não está vazio, nada
 * foi persistido ({@code importados} é sempre 0 nesse caso) — ver Blueprint (ROADMAP.md) de cada
 * módulo pra justificativa específica (ex.: entidade imutável sem endpoint de exclusão). Movido de
 * {@code com.sawhub.hub.financeiro.dto} pro pacote comum no M22, quando Mentorados e Comercial
 * passaram a ser o segundo e terceiro consumidor real — mesmo critério do M16 (centralizar na
 * segunda duplicação real, não esperar a terceira).*/
public record ImportResultResponse(int totalLinhas, int importados, List<ImportErro> erros) {
}
