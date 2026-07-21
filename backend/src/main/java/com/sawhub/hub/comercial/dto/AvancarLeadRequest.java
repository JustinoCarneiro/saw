package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.StatusLead;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** PATCH /leads/{id}/avancar — os campos além de {@code novoStatus} são opcionais e só fazem
 * sentido conforme o destino: {@code vendedorId} pra EM_CONTATO, {@code motivoPerdido} pra
 * PERDIDO. M28 — {@code novoStatus == FECHADO} sempre lança erro (ver LeadService.avancar()):
 * fechar venda de verdade é só via {@code fecharVenda()} (M25). Validado na entidade ({@link
 * com.sawhub.hub.comercial.Lead}), não aqui — o DTO só carrega o dado. */
public record AvancarLeadRequest(
        @NotNull StatusLead novoStatus,
        UUID vendedorId,
        String motivoPerdido
) {
}
