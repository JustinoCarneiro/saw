package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria_financeira")
public class CategoriaFinanceira extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLancamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_dre", nullable = false)
    private GrupoDre grupoDre;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_receita")
    private OrigemReceita origemReceita;

    protected CategoriaFinanceira() {
    }

    public CategoriaFinanceira(String nome, TipoLancamento tipo, GrupoDre grupoDre, OrigemReceita origemReceita) {
        this.nome = nome;
        this.tipo = tipo;
        this.grupoDre = grupoDre;
        this.origemReceita = origemReceita;
    }

    public String getNome() {
        return nome;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public GrupoDre getGrupoDre() {
        return grupoDre;
    }

    public OrigemReceita getOrigemReceita() {
        return origemReceita;
    }
}
