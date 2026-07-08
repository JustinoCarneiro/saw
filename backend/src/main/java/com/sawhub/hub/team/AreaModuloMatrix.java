package com.sawhub.hub.team;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Fonte única da matriz de permissões por área (E15/H15.2-H15.5). Login, o endpoint /me
 * e o endpoint /team/permission-matrix leem todos daqui — nunca duplicar esta tabela.
 */
public final class AreaModuloMatrix {

    private static final Map<Area, Set<Modulo>> MATRIX = new EnumMap<>(Area.class);

    static {
        MATRIX.put(Area.COMERCIAL, EnumSet.of(Modulo.COMERCIAL));
        MATRIX.put(Area.MARKETING, EnumSet.of(Modulo.CONTEUDOS));
        MATRIX.put(Area.GESTAO_PERFORMANCE, EnumSet.of(Modulo.MENTORADOS, Modulo.CONTEUDOS, Modulo.PAINEL_CONSOLIDADO));
        MATRIX.put(Area.FUNDADOR, EnumSet.allOf(Modulo.class));
    }

    private AreaModuloMatrix() {
    }

    public static Set<Modulo> allowedModulos(Area area) {
        return MATRIX.getOrDefault(area, Set.of());
    }

    public static boolean isAllowed(Area area, Modulo modulo) {
        return allowedModulos(area).contains(modulo);
    }

    public static Map<Area, Set<Modulo>> full() {
        return MATRIX;
    }
}
