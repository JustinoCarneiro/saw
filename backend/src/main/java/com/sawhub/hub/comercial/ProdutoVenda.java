package com.sawhub.hub.comercial;

/** M25 (change request pós-MVP, 17/07/2026) — o que foi vendido de fato, distinto de
 * {@link com.sawhub.hub.mentorado.TipoContrato} (M23, só cobre os tipos de mentoria/consultoria
 * que viram Mentorado). Catálogo confirmado nas 5 planilhas reais da operação — ver
 * docs/reuniao-2026-07-17-atualizacoes.md. {@code FORMULA_SAW} e {@code FORMACAO_PROFISSIONAL}
 * confirmados pelo Marcos em 18/07/2026 como categorias próprias (mesmo nível de
 * MENTORIA_CONTINUA/MENTORIA_INDIVIDUAL/CONSULTORIA — resolve as duas últimas Perguntas
 * pendentes do Blueprint M25/M23 em ROADMAP.md). {@code FICHA_TECNICA_LUCRATIVA} (gap 5, achado
 * via raio-x em "Vendas Aline Melo", confirmado 19/07/2026) — mesmo tratamento, categoria própria,
 * não vira {@link com.sawhub.hub.mentorado.TipoContrato}. */
public enum ProdutoVenda {
    MENTORIA_CONTINUA,
    MENTORIA_INDIVIDUAL,
    CONSULTORIA,
    FORMULA_SAW,
    FORMACAO_PROFISSIONAL,
    FICHA_TECNICA_LUCRATIVA,
    INGRESSO_EVENTO,
    PRODUTO_DIGITAL
}
