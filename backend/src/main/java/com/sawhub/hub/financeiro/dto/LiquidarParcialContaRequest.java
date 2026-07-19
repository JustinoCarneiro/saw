package com.sawhub.hub.financeiro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Gap 1 (raio-x, 18/07/2026) — pagamento parcial de uma conta, acumulativo (ver
 * {@link com.sawhub.hub.financeiro.ContaPagarReceber#liquidarParcial}). */
public record LiquidarParcialContaRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Valor pago deve ser positivo") BigDecimal valorPago,
        @NotNull LocalDate dataPagamento
) {
}
