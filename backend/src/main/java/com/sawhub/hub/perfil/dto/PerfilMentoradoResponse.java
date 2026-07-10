package com.sawhub.hub.perfil.dto;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public record PerfilMentoradoResponse(
        String nome,
        String negocio,
        String email,
        String telefone,
        String bio,
        List<String> areasInteresse,
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
                parseAreas(m.getAreasInteresse()),
                m.getFotoUrl(),
                m.getPlano(),
                m.getVencimentoPlano(),
                m.getCriadoEm()
        );
    }

    // Suposição 6 do Blueprint M15: CSV livre, sem taxonomia fixa — parse/serialize simétrico
    // aqui e em PerfilMentoradoService.atualizar().
    static List<String> parseAreas(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
