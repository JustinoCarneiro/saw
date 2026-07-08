package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.StatusLead;
import java.util.UUID;

/** Resposta do endpoint público (H1.3) — de propósito mínima, não é {@link LeadResponse}: quem
 * envia o formulário não está autenticado e não deve ver dado de pipeline comercial (vendedor,
 * outros campos internos). */
public record LeadCriadoResponse(UUID id, StatusLead status) {
    public static LeadCriadoResponse from(Lead l) {
        return new LeadCriadoResponse(l.getId(), l.getStatus());
    }
}
