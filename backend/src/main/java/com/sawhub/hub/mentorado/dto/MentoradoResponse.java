package com.sawhub.hub.mentorado.dto;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentorado.StatusMentorado;
import java.time.Instant;
import java.util.UUID;

public record MentoradoResponse(
        UUID id,
        String nome,
        String email,
        String negocio,
        Plano plano,
        StatusMentorado status,
        Instant criadoEm
) {
    public static MentoradoResponse from(Mentorado m) {
        return new MentoradoResponse(m.getId(), m.getNome(), m.getUsuario().getEmail(), m.getNegocio(),
                m.getPlano(), m.getStatus(), m.getCriadoEm());
    }
}
