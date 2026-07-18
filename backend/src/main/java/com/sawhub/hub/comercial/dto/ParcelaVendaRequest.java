package com.sawhub.hub.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** M25 — uma parcela do parcelamento estruturado; cada uma vira um ContaPagarReceber A_RECEBER
 * automaticamente (ver LeadService.fecharVenda). */
public record ParcelaVendaRequest(
        @NotNull @Positive Integer numero,
        @NotNull @Positive BigDecimal valor,
        @NotNull LocalDate dataPrevista
) {
}
