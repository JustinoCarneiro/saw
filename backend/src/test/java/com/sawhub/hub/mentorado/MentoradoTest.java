package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** E17/M27 (change request pós-MVP, 19/07/2026) — domínio das duas features aditivas do Blueprint
 * M27: as 4 ferramentas obrigatórias nomeadas (recálculo de ferramentasConcluidas/Total) e os
 * dois eixos de acompanhamento (semântica de PATCH: campo nulo não apaga valor já registrado). */
class MentoradoTest {

    private static Mentorado mentorado() {
        return new Mentorado(null, "Maria Souza", null, Plano.GRATUITO, BigDecimal.ZERO, 0, 0);
    }

    @Test
    void atualizarFerramentasObrigatoriasComAsQuatroEmSimContaAsQuatro() {
        Mentorado m = mentorado();

        m.atualizarFerramentasObrigatorias(EstadoImplementacao.SIM, EstadoImplementacao.SIM,
                EstadoImplementacao.SIM, EstadoImplementacao.SIM);

        assertThat(m.getFerramentasTotal()).isEqualTo(4);
        assertThat(m.getFerramentasConcluidas()).isEqualTo(4);
    }

    @Test
    void atualizarFerramentasObrigatoriasEmConstrucaoNaoContaComoConcluida() {
        Mentorado m = mentorado();

        m.atualizarFerramentasObrigatorias(EstadoImplementacao.EM_CONSTRUCAO, EstadoImplementacao.EM_CONSTRUCAO,
                EstadoImplementacao.NAO, EstadoImplementacao.NAO);

        assertThat(m.getFerramentasTotal()).isEqualTo(4);
        assertThat(m.getFerramentasConcluidas()).isZero();
    }

    @Test
    void atualizarFerramentasObrigatoriasComNuloVirandoNao() {
        Mentorado m = mentorado();

        m.atualizarFerramentasObrigatorias(null, null, null, null);

        assertThat(m.getFerramentaDre()).isEqualTo(EstadoImplementacao.NAO);
        assertThat(m.getFerramentasConcluidas()).isZero();
    }

    // Semântica de PATCH (ver Javadoc de Mentorado#atualizarAcompanhamento): campo nulo na
    // chamada não apaga o valor já registrado.
    @Test
    void atualizarAcompanhamentoComApenasUmEixoPreservaOOutroJaRegistrado() {
        Mentorado m = mentorado();
        m.atualizarAcompanhamento(NivelEngajamento.ALTO, RiscoChurn.NAO);

        m.atualizarAcompanhamento(null, RiscoChurn.ALTO);

        assertThat(m.getNivelEngajamento()).isEqualTo(NivelEngajamento.ALTO);
        assertThat(m.getRiscoChurn()).isEqualTo(RiscoChurn.ALTO);
    }

    @Test
    void atualizarAcompanhamentoMarcaADataDeAvaliacao() {
        Mentorado m = mentorado();
        assertThat(m.getAcompanhamentoAvaliadoEm()).isNull();

        m.atualizarAcompanhamento(NivelEngajamento.MEDIO, null);

        assertThat(m.getAcompanhamentoAvaliadoEm()).isNotNull();
    }
}
