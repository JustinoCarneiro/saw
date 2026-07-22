package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/** Change request pós-MVP (E14, "Caixa do mês: Inicial, saldo por banco, Final", reunião
 * 17/07/2026) — snapshot mensal de saldo por {@link ContaBancaria}, mesmo grão da aba mensal da
 * planilha real "DRE Financeira Saw" (uma aba por mês, saldo inicial/final digitado à mão — a
 * planilha não deriva isso de nenhum lançamento, é 100% preenchida manualmente, ver raio-x em
 * docs/reuniao-2026-07-17-atualizacoes.md). Por isso este registro também é entrada manual do
 * Admin (ver {@code CaixaMensalService.registrarPosicao}, upsert por conta+ano+mes), não
 * calculado a partir de {@link LancamentoFinanceiro}. */
@Entity
@Table(name = "posicao_caixa_mensal",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conta_bancaria_id", "ano", "mes"}))
public class PosicaoCaixaMensal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id", nullable = false)
    private ContaBancaria contaBancaria;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false)
    private int mes;

    @Column(name = "saldo_inicial", nullable = false)
    private BigDecimal saldoInicial;

    @Column(name = "saldo_final", nullable = false)
    private BigDecimal saldoFinal;

    protected PosicaoCaixaMensal() {
    }

    public PosicaoCaixaMensal(ContaBancaria contaBancaria, int ano, int mes, BigDecimal saldoInicial,
                               BigDecimal saldoFinal) {
        this.contaBancaria = contaBancaria;
        this.ano = ano;
        this.mes = mes;
        this.saldoInicial = saldoInicial;
        this.saldoFinal = saldoFinal;
    }

    public void atualizar(BigDecimal saldoInicial, BigDecimal saldoFinal) {
        this.saldoInicial = saldoInicial;
        this.saldoFinal = saldoFinal;
    }

    public ContaBancaria getContaBancaria() {
        return contaBancaria;
    }

    public int getAno() {
        return ano;
    }

    public int getMes() {
        return mes;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }
}
