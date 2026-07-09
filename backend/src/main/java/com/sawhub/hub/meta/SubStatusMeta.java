package com.sawhub.hub.meta;

/** Só existe enquanto {@link StatusMeta#ATIVA} — calculado a partir de prazo vs hoje, nunca
 * persistido (ver ROADMAP.md M09: mesmo padrão do E17, status derivado não é coluna própria). */
public enum SubStatusMeta {
    NO_PRAZO,
    ATENCAO,
    ATRASADA
}
