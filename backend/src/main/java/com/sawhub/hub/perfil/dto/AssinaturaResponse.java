package com.sawhub.hub.perfil.dto;

import com.sawhub.hub.mentorado.Plano;
import java.time.LocalDate;
import java.util.List;

/** H9.3 — informativo (ver Suposição 5 do Blueprint M15); troca de plano de fato é admin-only. */
public record AssinaturaResponse(
        Plano planoAtual,
        LocalDate vencimentoPlano,
        List<PlanoDisponivel> planosDisponiveis
) {
    public record PlanoDisponivel(Plano plano, boolean acimaDoPlanoAtual) {
    }
}
