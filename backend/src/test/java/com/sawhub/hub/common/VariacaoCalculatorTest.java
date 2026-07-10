package com.sawhub.hub.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VariacaoCalculatorTest {

    @Test
    void calculaVariacaoPositivaEntreDoisValores() {
        assertThat(VariacaoCalculator.pct(new BigDecimal("100"), new BigDecimal("120"))).isEqualTo(20.0);
    }

    @Test
    void calculaVariacaoNegativaEntreDoisValores() {
        assertThat(VariacaoCalculator.pct(new BigDecimal("200"), new BigDecimal("150"))).isEqualTo(-25.0);
    }

    @Test
    void anteriorZeroRetornaZeroEmVezDeDivisaoPorZero() {
        assertThat(VariacaoCalculator.pct(BigDecimal.ZERO, new BigDecimal("50"))).isZero();
    }

    @Test
    void overloadLongDelegaCorretamenteParaBigDecimal() {
        assertThat(VariacaoCalculator.pct(4L, 6L)).isEqualTo(50.0);
    }
}
