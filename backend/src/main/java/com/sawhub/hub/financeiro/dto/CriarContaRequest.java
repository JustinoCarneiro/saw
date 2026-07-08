package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.TipoConta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarContaRequest(
        @NotNull TipoConta tipo,
        @NotBlank String descricao,
        @NotNull @DecimalMin(value = "0.01", message = "Valor deve ser positivo") BigDecimal valor,
        @NotNull LocalDate dataVencimento,
        UUID categoriaId
) {
}
