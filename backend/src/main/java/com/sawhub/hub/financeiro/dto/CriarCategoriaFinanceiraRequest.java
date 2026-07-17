package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// origemReceita nullable de propósito: só faz sentido pra tipo RECEITA (ver OrigemReceita), não
// dá pra marcar @NotNull condicional em bean validation puro — validação cruzada fica no service.
public record CriarCategoriaFinanceiraRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotNull TipoLancamento tipo,
        @NotNull GrupoDre grupoDre,
        OrigemReceita origemReceita
) {
}
