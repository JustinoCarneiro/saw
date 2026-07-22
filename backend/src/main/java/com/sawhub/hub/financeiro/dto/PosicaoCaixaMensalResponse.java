package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.PosicaoCaixaMensal;
import java.math.BigDecimal;
import java.util.UUID;

public record PosicaoCaixaMensalResponse(
        UUID contaBancariaId,
        String contaBancariaNome,
        int ano,
        int mes,
        BigDecimal saldoInicial,
        BigDecimal saldoFinal
) {
    public static PosicaoCaixaMensalResponse from(PosicaoCaixaMensal posicao) {
        return new PosicaoCaixaMensalResponse(posicao.getContaBancaria().getId(), posicao.getContaBancaria().getNome(),
                posicao.getAno(), posicao.getMes(), posicao.getSaldoInicial(), posicao.getSaldoFinal());
    }
}
