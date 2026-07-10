package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// vencimentoPlano nullable (M15/E9, H9.3): setado pelo Admin junto do plano, opcional — nem todo
// plano/mentorado tem data de vencimento definida ainda nesta leva.
public record AtualizarMentoradoRequest(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 255) String negocio,
        @NotNull Plano plano,
        LocalDate vencimentoPlano
) {
}
