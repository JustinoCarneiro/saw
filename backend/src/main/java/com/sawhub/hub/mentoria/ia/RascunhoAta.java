package com.sawhub.hub.mentoria.ia;

// Auditoria de UX (22/07/2026) — perdeu o campo "encaminhamentos": no processo real da SAW
// (Notion) os encaminhamentos são digitados manualmente pelo mentor, não sugeridos por IA pra
// revisão (ver AtaEncaminhamentoSugerido). resumo/decisões continuam o diferencial de IA
// confirmado com o cliente (CLAUDE.md § "Diferenciais do MVP").
public record RascunhoAta(String resumo, String decisoes) {
}
