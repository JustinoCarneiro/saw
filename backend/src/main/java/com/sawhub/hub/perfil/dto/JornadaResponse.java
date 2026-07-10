package com.sawhub.hub.perfil.dto;

import com.sawhub.hub.perfil.NivelJornada;
import java.util.List;

/** H9.2 — XP e conquistas são calculados ao vivo, não persistidos (ver Suposições 2/3 do
 * Blueprint M15) — sem data de desbloqueio nas conquistas por esse motivo. */
public record JornadaResponse(
        NivelJornada nivelAtual,
        int xp,
        Integer xpProximoNivel,
        int progressoPct,
        Stats stats,
        List<Conquista> conquistas
) {
    public record Stats(
            long materiaisAcessados,
            long dicasAssistidas,
            long eventosParticipados,
            long mentoriasRealizadas
    ) {
    }

    public record Conquista(String codigo, String titulo, String descricao, boolean desbloqueada) {
    }
}
