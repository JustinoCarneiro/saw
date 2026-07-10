package com.sawhub.hub.perfil;

/** H9.2 — nível derivado do XP calculado em {@link PerfilJornadaService} (ver fórmula e
 * patamares no Blueprint M15, ROADMAP.md). Ordem declarada = hierarquia, mesmo padrão de
 * {@code Plano} (não reordenar). */
public enum NivelJornada {
    BRONZE(0),
    PRATA(1500),
    OURO(4000),
    DIAMANTE(8000);

    private final int xpMinimo;

    NivelJornada(int xpMinimo) {
        this.xpMinimo = xpMinimo;
    }

    public int getXpMinimo() {
        return xpMinimo;
    }

    public static NivelJornada paraXp(int xp) {
        NivelJornada atual = BRONZE;
        for (NivelJornada nivel : values()) {
            if (xp >= nivel.xpMinimo) {
                atual = nivel;
            }
        }
        return atual;
    }

    public NivelJornada proximo() {
        NivelJornada[] valores = values();
        int proximoOrdinal = ordinal() + 1;
        return proximoOrdinal < valores.length ? valores[proximoOrdinal] : null;
    }
}
