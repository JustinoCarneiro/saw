package com.sawhub.hub.consolidated.dto;

import java.math.BigDecimal;

public record RankingFaturamentoResponse(int pos, String nome, BigDecimal crescimentoFaturamentoPct) {
}
