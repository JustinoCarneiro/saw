package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Fórmula única de progresso ponderado (peso concluído / peso total), reusada pelo Painel
 * Consolidado do Admin (E17, {@code MentoradoConsolidadoResponse}) e pelo Dashboard do Mentorado
 * (M08) — mesmo mentorado tem que ver o mesmo número nos dois lugares, não duas contas que podem
 * divergir com o tempo.
 */
class ProgressoCalculatorTest {

    @Test
    void arredondaParaOInteiroMaisProximo() {
        // 6/9 = 66.666...% -> 67
        assertThat(ProgressoCalculator.pctPeso(6, 9)).isEqualTo(67);
    }

    @Test
    void semPesoTotal_zeroENaoDividePorZero() {
        assertThat(ProgressoCalculator.pctPeso(0, 0)).isZero();
    }

    @Test
    void cemPorCento() {
        assertThat(ProgressoCalculator.pctPeso(10, 10)).isEqualTo(100);
    }
}
