package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.TipoContrato;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** M23 — edição administrativa dos dados de contrato (H11.1 estendida). */
public record AtualizarDadosContratoRequest(
        @Size(max = 255) String nomeFantasia,
        @Pattern(regexp = "^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$|^\\d{14}$",
                message = "CNPJ deve estar no formato 00.000.000/0000-00 ou 14 dígitos")
        String cnpj,
        @Size(max = 500) String socios,
        TipoContrato tipoContrato,
        @PositiveOrZero BigDecimal valorContrato,
        LocalDate dataFechamentoContrato
) {
}
