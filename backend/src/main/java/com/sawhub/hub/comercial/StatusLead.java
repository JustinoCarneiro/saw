package com.sawhub.hub.comercial;

public enum StatusLead {
    SOLICITACAO,
    EM_CONTATO,
    // M25 (change request pós-MVP, 17/07/2026) — aditivo: bate com o funil real mostrado em
    // fluxograma_aline_comercial.pdf (Prospecção -> Contato -> Diagnóstico -> Proposta ->
    // Fechou?). Nenhum Lead antigo teve esse status, não precisa de migração de dado.
    DIAGNOSTICO,
    PROPOSTA,
    FECHADO,
    PERDIDO
}
