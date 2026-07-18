package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.EstadoImplementacao;
import com.sawhub.hub.mentorado.RespostaSimNao;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** M23 — Diagnóstico Inicial (feito pela Leia, papel "Sucesso do Gestor", antes da 1ª reunião do
 * mentorado com o Mateus). Todos os campos são opcionais — o levantamento real (Notion) mostra
 * diagnóstico preenchido de forma incremental, raramente tudo de uma vez. */
public record AtualizarDiagnosticoInicialRequest(
        BigDecimal faturamentoAnual,
        Integer quantidadeColaboradores,
        Boolean empresaRegularizada,
        Integer quantidadeLojas,
        RespostaSimNao cmvDefinido,
        @Size(max = 255) String cmvDetalhe,
        @Size(max = 100) String tempoMedioAtendimento,
        EstadoImplementacao culturaConstruida,
        EstadoImplementacao processosDesenhados
) {
}
