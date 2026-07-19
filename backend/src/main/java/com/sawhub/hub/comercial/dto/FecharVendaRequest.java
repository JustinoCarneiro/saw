package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.FormaPagamento;
import com.sawhub.hub.comercial.OrigemVenda;
import com.sawhub.hub.comercial.ProdutoVenda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** M25 — "formulário único de venda" (pedido explícito do cliente: distribui o dado
 * automaticamente pro financeiro/credenciamento, em vez de 2-3 planilhas separadas).
 * {@code eventoId}/{@code ingressos} só fazem sentido quando {@code produtoVenda} =
 * INGRESSO_EVENTO (validado em {@code LeadService.fecharVenda}, não aqui — depende do valor de
 * outro campo, Bean Validation simples não cobre isso limpo). */
public record FecharVendaRequest(
        @NotNull ProdutoVenda produtoVenda,
        @NotNull OrigemVenda origemVenda,
        @NotNull @PositiveOrZero BigDecimal valorTotalVenda,
        @PositiveOrZero BigDecimal valorPagoNoAto,
        @NotNull FormaPagamento formaPagamento,
        List<@Valid ParcelaVendaRequest> parcelas,
        UUID eventoId,
        List<@Valid VendaIngressoRequest> ingressos,
        // Gap 7 (confirmado 19/07/2026) — valor retido pelo gateway (Hotmart etc.) antes de
        // repassar pra SAW. Construtor de 8 args abaixo mantém todo chamador anterior compilando.
        @PositiveOrZero BigDecimal taxaPlataformaRetida
) {
    public FecharVendaRequest(ProdutoVenda produtoVenda, OrigemVenda origemVenda, BigDecimal valorTotalVenda,
                               BigDecimal valorPagoNoAto, FormaPagamento formaPagamento,
                               List<ParcelaVendaRequest> parcelas, UUID eventoId, List<VendaIngressoRequest> ingressos) {
        this(produtoVenda, origemVenda, valorTotalVenda, valorPagoNoAto, formaPagamento, parcelas, eventoId,
                ingressos, null);
    }
}
