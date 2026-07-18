package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.TipoContrato;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** M23 — "criar mentorado direto" (pedido explícito do cliente: "É IMPORTANTE PODER CRIAR
 * DIRETAMENTE O MENTORADO, SENDO AUTOMÁTICAMENTE UM LEAD FECHADO"). Cria Lead+Usuario+Mentorado
 * numa tacada só, sem exigir um Lead pré-existente no funil. */
public record CriarMentoradoDiretoRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 255) String negocio,
        @Size(max = 30) String telefone,
        @NotNull TipoContrato tipoContrato,
        @PositiveOrZero BigDecimal valorContrato,
        LocalDate dataFechamentoContrato
) {
}
