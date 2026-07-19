package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.ContaPagarReceber;
import com.sawhub.hub.financeiro.StatusConta;
import com.sawhub.hub.financeiro.TipoConta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        TipoConta tipo,
        String descricao,
        BigDecimal valor,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        StatusConta status,
        BigDecimal valorPago,
        UUID lancamentoId
) {
    public static ContaResponse from(ContaPagarReceber c) {
        return new ContaResponse(c.getId(), c.getTipo(), c.getDescricao(), c.getValor(), c.getDataVencimento(),
                c.getDataPagamento(), c.getStatus(), c.getValorPago(),
                c.getLancamento() == null ? null : c.getLancamento().getId());
    }
}
