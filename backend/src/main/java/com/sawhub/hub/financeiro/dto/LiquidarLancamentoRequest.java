package com.sawhub.hub.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** M26 (renomeado de `LiquidarContaRequest`) — sem `criarLancamento`: liquidar sempre é mutar o
 * próprio lançamento pra REALIZADO, não existe mais um segundo registro a gerar opcionalmente. */
public record LiquidarLancamentoRequest(@NotNull LocalDate dataPagamento) {
}
