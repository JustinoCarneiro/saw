package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.ContaBancaria;
import java.util.UUID;

public record ContaBancariaResponse(UUID id, String nome, boolean ativa) {
    public static ContaBancariaResponse from(ContaBancaria conta) {
        return new ContaBancariaResponse(conta.getId(), conta.getNome(), conta.isAtiva());
    }
}
