package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.NaturezaFinanceira;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// origemReceita nullable de propósito: só faz sentido pra tipo RECEITA (ver OrigemReceita), não
// dá pra marcar @NotNull condicional em bean validation puro — validação cruzada fica no service.
// grupo/natureza também nullable (E14 — subcategorias fixo/variável, ver CategoriaFinanceira).
public record CriarCategoriaFinanceiraRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotNull TipoLancamento tipo,
        @NotNull GrupoDre grupoDre,
        OrigemReceita origemReceita,
        @Size(max = 60) String grupo,
        NaturezaFinanceira natureza
) {
    public CriarCategoriaFinanceiraRequest(String nome, TipoLancamento tipo, GrupoDre grupoDre,
                                            OrigemReceita origemReceita) {
        this(nome, tipo, grupoDre, origemReceita, null, null);
    }
}
