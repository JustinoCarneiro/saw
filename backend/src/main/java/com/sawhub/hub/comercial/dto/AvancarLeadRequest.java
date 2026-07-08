package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.mentorado.Plano;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** PATCH /leads/{id}/avancar — os campos além de {@code novoStatus} são opcionais e só fazem
 * sentido conforme o destino: {@code vendedorId} pra EM_CONTATO, {@code planoFechado} pra
 * FECHADO, {@code motivoPerdido} pra PERDIDO. Validado na entidade ({@link
 * com.sawhub.hub.comercial.Lead}), não aqui — o DTO só carrega o dado. */
public record AvancarLeadRequest(
        @NotNull StatusLead novoStatus,
        UUID vendedorId,
        Plano planoFechado,
        String motivoPerdido
) {
}
