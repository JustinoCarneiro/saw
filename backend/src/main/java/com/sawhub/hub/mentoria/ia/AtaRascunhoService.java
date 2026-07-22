package com.sawhub.hub.mentoria.ia;

/** Porta pro provedor de geração do rascunho (Claude Sonnet 5, decisão do ROADMAP.md M06) —
 * resumo + decisões a partir da transcrição. */
public interface AtaRascunhoService {
    RascunhoAta gerarRascunho(String transcricao);
}
