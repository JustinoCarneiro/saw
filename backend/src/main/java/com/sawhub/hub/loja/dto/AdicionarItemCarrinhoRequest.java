package com.sawhub.hub.loja.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

// Achado (baixo) do revisor-seguranca no M14: sem teto, uma quantidade absurda (ex.: 2 bilhões)
// não forja preço (ainda multiplica o precoUnitario travado no servidor), mas gera um item com
// total gigantesco enviado à Preferences API do Mercado Pago sem necessidade real de compra.
public record AdicionarItemCarrinhoRequest(@NotNull UUID produtoId, @NotNull @Positive @Max(20) Integer quantidade) {
}
