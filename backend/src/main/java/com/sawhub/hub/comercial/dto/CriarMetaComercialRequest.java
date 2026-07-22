package com.sawhub.hub.comercial.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Pedido do Marcos (22/07/2026) — até aqui não existia endpoint nenhum pra definir meta
 * comercial, só o seed de demonstração; o Ranking ficava mudo pra qualquer vendedor/mês sem uma
 * linha plantada manualmente no banco. */
public record CriarMetaComercialRequest(
        @NotNull UUID vendedorId,
        @Min(2020) int ano,
        @Min(1) @Max(12) int mes,
        @Min(0) int metaFechamentos,
        @Min(0) @Max(100) BigDecimal percentualComissao
) {
}
