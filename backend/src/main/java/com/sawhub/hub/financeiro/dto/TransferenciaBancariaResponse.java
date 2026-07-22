package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.TransferenciaBancaria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferenciaBancariaResponse(
        UUID id,
        UUID contaOrigemId,
        String contaOrigemNome,
        UUID contaDestinoId,
        String contaDestinoNome,
        BigDecimal valor,
        LocalDate data,
        String descricao
) {
    public static TransferenciaBancariaResponse from(TransferenciaBancaria t) {
        return new TransferenciaBancariaResponse(t.getId(),
                t.getContaOrigem().getId(), t.getContaOrigem().getNome(),
                t.getContaDestino().getId(), t.getContaDestino().getNome(),
                t.getValor(), t.getData(), t.getDescricao());
    }
}
