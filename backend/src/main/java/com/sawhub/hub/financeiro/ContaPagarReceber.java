package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** H14.4 — conta a pagar/receber. Máquina de estado: {@link StatusConta#PENDENTE} -&gt;
 * {@link StatusConta#PAGO}/{@link StatusConta#RECEBIDO} (ou desvio -&gt;
 * {@link StatusConta#VENCIDO}). A transição vive na entidade de propósito — impossível
 * alguém fora daqui criar um estado tipo/status inconsistente (ex.: A_PAGAR virando RECEBIDO). */
@Entity
@Table(name = "conta_pagar_receber")
public class ContaPagarReceber extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConta tipo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusConta status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaFinanceira categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_id")
    private LancamentoFinanceiro lancamento;

    protected ContaPagarReceber() {
    }

    public ContaPagarReceber(TipoConta tipo, String descricao, BigDecimal valor, LocalDate dataVencimento,
                              CategoriaFinanceira categoria) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.categoria = categoria;
        this.status = StatusConta.PENDENTE;
    }

    /** Só permitido a partir de PENDENTE — ver {@link IllegalStateException} pra qualquer outra
     * transição (ex.: liquidar uma conta já VENCIDA precisa passar por essa checagem também,
     * é uma decisão de negócio válida, só não pode vir de nenhum outro status). */
    public void liquidar(LocalDate dataPagamento, LancamentoFinanceiro lancamentoGerado) {
        if (status == StatusConta.PAGO || status == StatusConta.RECEBIDO) {
            throw new IllegalStateException("Esta conta já foi liquidada.");
        }
        this.dataPagamento = dataPagamento;
        this.status = tipo == TipoConta.A_PAGAR ? StatusConta.PAGO : StatusConta.RECEBIDO;
        this.lancamento = lancamentoGerado;
    }

    public void marcarVencida() {
        if (status == StatusConta.PENDENTE) {
            this.status = StatusConta.VENCIDO;
        }
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public StatusConta getStatus() {
        return status;
    }

    public LancamentoFinanceiro getLancamento() {
        return lancamento;
    }

    public CategoriaFinanceira getCategoria() {
        return categoria;
    }
}
