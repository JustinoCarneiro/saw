package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.OrigemReceita;
import java.math.BigDecimal;

public record ComposicaoReceita(OrigemReceita origem, BigDecimal valor) {
}
