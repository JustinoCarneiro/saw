package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.Lead;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.mentorado.Plano;
import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        String mensagem,
        Plano planoInteresse,
        StatusLead status,
        VendedorResumo vendedor,
        Plano planoFechado,
        String motivoPerdido,
        Instant dataFechamento,
        Instant criadoEm
) {
    public static LeadResponse from(Lead l) {
        return new LeadResponse(l.getId(), l.getNome(), l.getEmail(), l.getTelefone(), l.getMensagem(),
                l.getPlanoInteresse(), l.getStatus(), VendedorResumo.from(l.getVendedor()), l.getPlanoFechado(),
                l.getMotivoPerdido(), l.getDataFechamento(), l.getCriadoEm());
    }
}
