package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.FormaPagamentoLancamento;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoResponse(
        UUID id,
        TipoLancamento tipo,
        CategoriaResumo categoria,
        String descricao,
        BigDecimal valor,
        LocalDate dataCompetencia,
        StatusLancamento status,
        UUID eventoId,
        String eventoTitulo,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        BigDecimal valorPago,
        FormaPagamentoLancamento formaPagamento
) {
    public record CategoriaResumo(UUID id, String nome, OrigemReceita origemReceita) {
    }

    public static LancamentoResponse from(LancamentoFinanceiro l) {
        var categoria = new CategoriaResumo(l.getCategoria().getId(), l.getCategoria().getNome(),
                l.getCategoria().getOrigemReceita());
        UUID eventoId = l.getEvento() != null ? l.getEvento().getId() : null;
        String eventoTitulo = l.getEvento() != null ? l.getEvento().getTitulo() : null;
        return new LancamentoResponse(l.getId(), l.getTipo(), categoria, l.getDescricao(), l.getValor(),
                l.getDataCompetencia(), l.getStatus(), eventoId, eventoTitulo,
                l.getDataVencimento(), l.getDataPagamento(), l.getValorPago(), l.getFormaPagamento());
    }
}
