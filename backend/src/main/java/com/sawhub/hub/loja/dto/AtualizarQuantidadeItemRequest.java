package com.sawhub.hub.loja.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Mesmo achado do AdicionarItemCarrinhoRequest — ver comentário lá.
public record AtualizarQuantidadeItemRequest(@NotNull @Positive @Max(20) Integer quantidade) {
}
