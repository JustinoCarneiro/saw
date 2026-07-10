package com.sawhub.hub.aviso.dto;

import com.sawhub.hub.aviso.Aviso;
import com.sawhub.hub.aviso.AvisoMentorado;
import com.sawhub.hub.aviso.CategoriaAviso;
import java.time.Instant;
import java.util.UUID;

public record AvisoMentoradoResponse(
        UUID id,
        String titulo,
        String descricao,
        CategoriaAviso categoria,
        boolean lido,
        Instant quando
) {
    public static AvisoMentoradoResponse from(Aviso a, AvisoMentorado am) {
        return new AvisoMentoradoResponse(a.getId(), a.getTitulo(), a.getDescricao(), a.getCategoria(),
                am != null && am.isLido(), a.getCriadoEm());
    }
}
