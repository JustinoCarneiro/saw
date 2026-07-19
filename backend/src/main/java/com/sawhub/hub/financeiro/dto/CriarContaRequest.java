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
        UUID categoriaId,
        // Change request 17/07/2026 ("evento rastreado no financeiro") — opcional.
        UUID eventoId
) {
    public CriarContaRequest(TipoConta tipo, String descricao, BigDecimal valor, LocalDate dataVencimento,
                              UUID categoriaId) {
        this(tipo, descricao, valor, dataVencimento, categoriaId, null);
    }
}
