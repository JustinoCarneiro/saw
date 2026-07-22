package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.MetaComercial;
import java.math.BigDecimal;
import java.util.UUID;

public record MetaComercialResponse(UUID vendedorId, String vendedorNome, int metaFechamentos, BigDecimal percentualComissao) {
    public static MetaComercialResponse from(MetaComercial meta) {
        return new MetaComercialResponse(meta.getVendedor().getId(), meta.getVendedor().getNome(), meta.getMetaFechamentos(), meta.getPercentualComissao());
    }
}
