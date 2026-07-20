package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.NivelEngajamento;
import com.sawhub.hub.mentorado.RiscoChurn;

/** E17/M27 — "dois eixos de acompanhamento", preenchimento manual (ver ROADMAP.md § "Blueprint
 * (M27)"). Ambos nullable — mesma nullability do campo na entidade, dá pra registrar só um dos
 * dois eixos numa avaliação. */
public record AtualizarAcompanhamentoRequest(NivelEngajamento nivelEngajamento, RiscoChurn riscoChurn) {
}
