package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarLancamentoRequest(
        @NotNull TipoLancamento tipo,
        @NotNull UUID categoriaId,
        @NotBlank String descricao,
        @NotNull @DecimalMin(value = "0.01", message = "Valor deve ser positivo") BigDecimal valor,
        @NotNull LocalDate dataCompetencia,
        @NotNull StatusLancamento status,
        Plano planoReferencia
) {
}
