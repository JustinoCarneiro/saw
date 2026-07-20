package com.sawhub.hub.financeiro.dto;

import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.NaturezaFinanceira;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.TipoLancamento;
import java.util.UUID;

public record CategoriaResponse(UUID id, String nome, TipoLancamento tipo, GrupoDre grupoDre,
                                 OrigemReceita origemReceita, String grupo, NaturezaFinanceira natureza) {
    public static CategoriaResponse from(CategoriaFinanceira c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getTipo(), c.getGrupoDre(), c.getOrigemReceita(),
                c.getGrupo(), c.getNatureza());
    }
}
