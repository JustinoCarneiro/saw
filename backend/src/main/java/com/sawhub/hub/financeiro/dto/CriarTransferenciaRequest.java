package com.sawhub.hub.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarTransferenciaRequest(
        @NotNull UUID contaOrigemId,
        @NotNull UUID contaDestinoId,
        @NotNull @Positive BigDecimal valor,
        @NotNull LocalDate data,
        @Size(max = 255) String descricao
) {
}
