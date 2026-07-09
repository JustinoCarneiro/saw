package com.sawhub.hub.evento;

/** CLAUDE.md § Máquinas de estado: "Inscrição em evento: Disponível -> Inscrito -> Participado
 * (desvio: -> Cancelada)". "Disponível" nunca é persistido — é só "a linha ainda não existe". */
public enum StatusInscricao {
    INSCRITA,
    CANCELADA,
    PARTICIPOU
}
