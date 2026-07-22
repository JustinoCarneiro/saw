package com.sawhub.hub.financeiro.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RegistrarPosicaoCaixaRequest(
        @NotNull UUID contaBancariaId,
        @Min(2020) int ano,
        @Min(1) @Max(12) int mes,
        @NotNull BigDecimal saldoInicial,
        @NotNull BigDecimal saldoFinal
) {
}
