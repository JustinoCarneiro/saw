package com.sawhub.hub.mentorado;

/**
 * Fórmula única de progresso ponderado (peso concluído / peso total dos {@link Encaminhamento}),
 * reusada pelo Painel Consolidado do Admin (E17) e pelo Dashboard do Mentorado (M08) — mesmo
 * mentorado tem que ver o mesmo número nos dois lugares, não duas contas que podem divergir.
 */
public final class ProgressoCalculator {

    private ProgressoCalculator() {
    }

    public static int pctPeso(long pesoConcluido, long pesoTotal) {
        return pesoTotal == 0 ? 0 : (int) Math.round(pesoConcluido * 100.0 / pesoTotal);
    }
}
