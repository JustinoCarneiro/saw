package com.sawhub.hub.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Centralizado no M16 (E10): a mesma fórmula já existia duplicada em RelatorioFinanceiroService
// (E14) e ComercialDashboardService (E13) — E10 seria o terceiro ponto a reimplementá-la. Mesma
// disciplina do ProgressoCalculator/Plano.atendePlanoMinimo (M08/M11): centralizar assim que a
// duplicata é encontrada, não deixar "pra depois".
public final class VariacaoCalculator {

    private VariacaoCalculator() {
    }

    public static double pct(BigDecimal anterior, BigDecimal atual) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return atual.subtract(anterior)
                .divide(anterior.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public static double pct(long anterior, long atual) {
        return pct(BigDecimal.valueOf(anterior), BigDecimal.valueOf(atual));
    }
}
