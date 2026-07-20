package com.sawhub.hub.financeiro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** M26 (renomeado de `LiquidarParcialContaRequest`) — pagamento parcial de um lançamento,
 * acumulativo (ver {@link com.sawhub.hub.financeiro.LancamentoFinanceiro#liquidarParcial}). */
public record LiquidarParcialLancamentoRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Valor pago deve ser positivo") BigDecimal valorPago,
        @NotNull LocalDate dataPagamento
) {
}
