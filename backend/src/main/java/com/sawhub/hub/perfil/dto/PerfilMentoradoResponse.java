package com.sawhub.hub.perfil.dto;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import java.time.Instant;
import java.time.LocalDate;

public record PerfilMentoradoResponse(
        String nome,
        String negocio,
        String email,
        String telefone,
        String bio,
        String fotoUrl,
        Plano plano,
        LocalDate vencimentoPlano,
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
                m.getPlano(),
                m.getVencimentoPlano(),
                m.getCriadoEm()
        );
    }
}
