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

    // E14 — achado no raio-x da planilha real "DRE Financeira Saw" (ver docs/reuniao-2026-07-17-
    // atualizacoes.md). `grupo` é texto livre de propósito (não enum): o vocabulário de
    // agrupamento já é diferente entre receita e despesa na planilha real ("Estrutura"/"Pessoas"
    // pra despesa, "Vendas"/"Mentoria" pra receita) — um enum único obrigaria valores que não
    // fazem sentido pro outro lado.
    @Column(name = "grupo")
    private String grupo;

    // Fixa/Variável é consistente por subcategoria (mesma subcategoria sempre repete o mesmo
    // valor nas linhas reais da planilha), não uma escolha livre por lançamento — por isso vive
    // aqui, não em LancamentoFinanceiro. Nullable: nem toda categoria se encaixa na dicotomia
    // (ex. as ligadas a evento, que na planilha caem no próprio bucket "Eventos").
    @Enumerated(EnumType.STRING)
    @Column(name = "natureza")
    private NaturezaFinanceira natureza;

    protected CategoriaFinanceira() {
    }

    public CategoriaFinanceira(String nome, TipoLancamento tipo, GrupoDre grupoDre, OrigemReceita origemReceita) {
        this(nome, tipo, grupoDre, origemReceita, null, null);
    }

    public CategoriaFinanceira(String nome, TipoLancamento tipo, GrupoDre grupoDre, OrigemReceita origemReceita,
                                String grupo, NaturezaFinanceira natureza) {
        this.nome = nome;
        this.tipo = tipo;
        this.grupoDre = grupoDre;
        this.origemReceita = origemReceita;
        this.grupo = grupo;
        this.natureza = natureza;
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

    public String getGrupo() {
        return grupo;
    }

    public NaturezaFinanceira getNatureza() {
        return natureza;
    }
}
