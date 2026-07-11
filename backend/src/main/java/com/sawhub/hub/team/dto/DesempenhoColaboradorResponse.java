package com.sawhub.hub.team.dto;

import com.sawhub.hub.team.Colaborador;

/** H15.7 (M20) — desempenho do time no período: mentoriasRealizadas é computado pra todo mundo;
 * metaFechamentos/fechamentosRealizados/pctAtingido só existem pra quem já tem MetaComercial
 * configurada no período (hoje, só área Comercial) — nulos pros demais, sem inventar uma nova
 * meta de mentorias pra quem não tem (ver Suposição 3 no Blueprint, ROADMAP.md). */
public record DesempenhoColaboradorResponse(String id, String nome, String area, long mentoriasRealizadas,
                                              Integer metaFechamentos, Long fechamentosRealizados,
                                              Double pctAtingidoFechamentos) {

    public static DesempenhoColaboradorResponse semMeta(Colaborador colaborador, long mentoriasRealizadas) {
        return new DesempenhoColaboradorResponse(colaborador.getId().toString(), colaborador.getNome(),
                colaborador.getArea().name(), mentoriasRealizadas, null, null, null);
    }

    public static DesempenhoColaboradorResponse comMeta(Colaborador colaborador, long mentoriasRealizadas,
                                                          Integer metaFechamentos, Long fechamentosRealizados,
                                                          Double pctAtingidoFechamentos) {
        return new DesempenhoColaboradorResponse(colaborador.getId().toString(), colaborador.getNome(),
                colaborador.getArea().name(), mentoriasRealizadas, metaFechamentos, fechamentosRealizados,
                pctAtingidoFechamentos);
    }
}
