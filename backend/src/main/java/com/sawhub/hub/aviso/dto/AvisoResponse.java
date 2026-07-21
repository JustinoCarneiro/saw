package com.sawhub.hub.aviso.dto;

import com.sawhub.hub.aviso.Aviso;
import com.sawhub.hub.aviso.CategoriaAviso;
import java.time.Instant;
import java.util.UUID;

public record AvisoResponse(
        UUID id,
        String titulo,
        String descricao,
        CategoriaAviso categoria,
        Instant criadoEm
) {
    public static AvisoResponse from(Aviso a) {
        return new AvisoResponse(a.getId(), a.getTitulo(), a.getDescricao(), a.getCategoria(), a.getCriadoEm());
    }
}
