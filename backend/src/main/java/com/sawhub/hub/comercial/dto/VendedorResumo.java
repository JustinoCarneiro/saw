package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.team.Colaborador;
import java.util.UUID;

public record VendedorResumo(UUID id, String nome) {
    public static VendedorResumo from(Colaborador c) {
        return c == null ? null : new VendedorResumo(c.getId(), c.getNome());
    }
}
