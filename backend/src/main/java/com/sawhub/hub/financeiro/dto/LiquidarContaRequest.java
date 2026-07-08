package com.sawhub.hub.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LiquidarContaRequest(@NotNull LocalDate dataPagamento, boolean criarLancamento) {
}
