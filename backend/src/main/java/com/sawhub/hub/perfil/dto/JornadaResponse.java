package com.sawhub.hub.perfil.dto;

import com.sawhub.hub.perfil.NivelJornada;
import java.time.Instant;
import java.util.List;

/** H9.2 — XP e conquistas são calculados ao vivo (não persistidos, ver Suposições 2/3 do
 * Blueprint M15); a data de desbloqueio de cada conquista, sim (ver ConquistaDesbloqueada) —
 * {@code desbloqueadaEm} nulo com {@code desbloqueada=true} significa "desde sempre" (já era
 * verdadeira antes do rastreamento existir), não "sem data". */
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

    public record Conquista(String codigo, String titulo, String descricao, boolean desbloqueada, Instant desbloqueadaEm) {
    }
}
