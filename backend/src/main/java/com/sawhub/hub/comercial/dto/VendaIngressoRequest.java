package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.CategoriaIngresso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** M25 — um ingresso vendido (credenciamento nominal, pode ser pessoa diferente de quem comprou). */
public record VendaIngressoRequest(
        @NotNull CategoriaIngresso categoriaIngresso,
        @Size(max = 255) String nomeCredenciado,
        @Size(max = 100) String setor,
        boolean almoco
) {
}
