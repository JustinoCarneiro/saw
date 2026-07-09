package com.sawhub.hub.mentoria.dto;

import com.sawhub.hub.mentoria.AtaEncaminhamentoSugerido;
import java.util.UUID;

public record SugestaoResponse(UUID id, String titulo, Integer pesoSugerido, boolean aceito) {
    public static SugestaoResponse from(AtaEncaminhamentoSugerido s) {
        return new SugestaoResponse(s.getId(), s.getTitulo(), s.getPesoSugerido(), s.isAceito());
    }
}
