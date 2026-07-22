package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Change request pós-MVP (E14, "Caixa do mês: saldo por banco", reunião 17/07/2026) — conta
 * bancária real da SAW (ex.: Itaú, Infinity Pay, ver planilha real "DRE Financeira Saw"). Cadastro
 * simples via CRUD (mesmo padrão de {@link CategoriaFinanceiraController}), não ligada a
 * {@link LancamentoFinanceiro} — a planilha de origem também não amarra lançamento a banco, só
 * rastreia o saldo mensal por conta (ver {@link PosicaoCaixaMensal}). */
@Entity
@Table(name = "conta_bancaria")
public class ContaBancaria extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private boolean ativa;

    protected ContaBancaria() {
    }

    public ContaBancaria(String nome) {
        this.nome = nome;
        this.ativa = true;
    }

    public void desativar() {
        this.ativa = false;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
