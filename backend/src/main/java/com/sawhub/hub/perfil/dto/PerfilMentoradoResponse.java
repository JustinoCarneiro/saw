package com.sawhub.hub.perfil.dto;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.TipoContrato;
import java.time.Instant;

public record PerfilMentoradoResponse(
        String nome,
        String negocio,
        String email,
        String telefone,
        String bio,
        String fotoUrl,
        TipoContrato tipoContrato,
        Instant membroDesde
) {
    public static PerfilMentoradoResponse from(Mentorado m) {
        return new PerfilMentoradoResponse(
                m.getNome(),
                m.getNegocio(),
                m.getUsuario().getEmail(),
                m.getTelefone(),
                m.getBio(),
                m.getFotoUrl(),
                m.getTipoContrato(),
                m.getCriadoEm()
        );
    }
}
