package com.sawhub.hub.financeiro;

/** E14 — achado no raio-x da planilha real "DRE Financeira Saw": Fixa/Variável é consistente por
 * subcategoria (mesma subcategoria sempre repete o mesmo valor nas linhas reais), não uma escolha
 * livre por lançamento — por isso vive em {@link CategoriaFinanceira}, não em
 * {@link LancamentoFinanceiro}. Nullable na categoria: nem toda categoria (ex. as ligadas a
 * evento) se encaixa nessa dicotomia. */
public enum NaturezaFinanceira {
    FIXA,
    VARIAVEL
}
