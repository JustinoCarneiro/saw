package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.EstadoImplementacao;
import com.sawhub.hub.mentorado.MentoradoDiagnosticoInicial;
import com.sawhub.hub.mentorado.RespostaSimNao;
import java.math.BigDecimal;

public record DiagnosticoInicialResponse(
        BigDecimal faturamentoAnual,
        Integer quantidadeColaboradores,
        Boolean empresaRegularizada,
        Integer quantidadeLojas,
        RespostaSimNao cmvDefinido,
        String cmvDetalhe,
        String tempoMedioAtendimento,
        EstadoImplementacao culturaConstruida,
        EstadoImplementacao processosDesenhados
) {
    public static DiagnosticoInicialResponse from(MentoradoDiagnosticoInicial d) {
        return new DiagnosticoInicialResponse(d.getFaturamentoAnual(), d.getQuantidadeColaboradores(),
                d.getEmpresaRegularizada(), d.getQuantidadeLojas(), d.getCmvDefinido(), d.getCmvDetalhe(),
                d.getTempoMedioAtendimento(), d.getCulturaConstruida(), d.getProcessosDesenhados());
    }

    /** A Leia ainda não preencheu o Diagnóstico Inicial desse mentorado. */
    public static DiagnosticoInicialResponse vazio() {
        return new DiagnosticoInicialResponse(null, null, null, null, null, null, null, null, null);
    }
}
