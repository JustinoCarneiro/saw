package com.sawhub.hub.team.dto;

import com.sawhub.hub.team.Colaborador;
import java.util.UUID;

// H15.6 (M20): carteira agora é sempre computada (TeamService), não um campo do Colaborador —
// from() exige o valor já calculado como parâmetro, não lê mais da entidade.
public record ColaboradorResponse(UUID id, String nome, String email, String area, long carteira) {

    public static ColaboradorResponse from(Colaborador colaborador, long carteira) {
        return new ColaboradorResponse(
                colaborador.getId(),
                colaborador.getNome(),
                colaborador.getUsuario().getEmail(),
                colaborador.getArea().name(),
                carteira
        );
    }
}
