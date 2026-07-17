package com.sawhub.hub.common;

import java.util.Arrays;
import java.util.List;

/** Parse/serialize simétrico do campo livre "áreas de interesse" do mentorado (Suposição 6 do
 * Blueprint M15: CSV livre, sem taxonomia fixa) — armazenado como string única no banco, exposto
 * como lista nas APIs. Centralizado aqui porque tanto a autoedição do mentorado (H9.1,
 * {@code PerfilMentoradoService}) quanto a edição administrativa ({@code MentoradoAdminService})
 * precisam do mesmo par parse/join. */
public final class AreasInteresseUtil {

    private AreasInteresseUtil() {
    }

    public static List<String> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String join(List<String> areas) {
        return areas == null ? null : String.join(", ", areas);
    }
}
