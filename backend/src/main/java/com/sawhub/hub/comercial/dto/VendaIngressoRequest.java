package com.sawhub.hub.comercial.dto;

import com.sawhub.hub.comercial.CategoriaIngresso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** M25 — um ingresso vendido (credenciamento nominal, pode ser pessoa diferente de quem comprou).
 * nomeEmpresa/telefone/email (gap 3, 19/07/2026) são opcionais — nem toda venda real na planilha
 * tem os 3 preenchidos. */
public record VendaIngressoRequest(
        @NotNull CategoriaIngresso categoriaIngresso,
        @Size(max = 255) String nomeCredenciado,
        @Size(max = 100) String setor,
        boolean almoco,
        @Size(max = 255) String nomeEmpresa,
        @Size(max = 20) String telefone,
        @Email @Size(max = 255) String email
) {
}
