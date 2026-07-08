package com.sawhub.hub.team.dto;

import com.sawhub.hub.team.Colaborador;
import java.math.BigDecimal;
import java.util.UUID;

public record ColaboradorResponse(UUID id, String nome, String email, String area,
                                   Integer carteira, BigDecimal conversaoPct) {

    public static ColaboradorResponse from(Colaborador colaborador) {
        return new ColaboradorResponse(
                colaborador.getId(),
                colaborador.getNome(),
                colaborador.getUsuario().getEmail(),
                colaborador.getArea().name(),
                colaborador.getCarteira(),
                colaborador.getConversaoPct()
        );
    }
}
