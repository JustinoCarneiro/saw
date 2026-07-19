package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.evento.Evento;
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
import org.hibernate.annotations.ColumnTransformer;

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

    // pgcrypto (achado alto do revisor-seguranca, M25): descricao costuma conter nome de lead/
    // mentorado (ex. "Parcela 1 - Maria Souza") — PII já protegida na tabela de origem (V19),
    // criptografar aqui também fecha o buraco de reintroduzir esse mesmo dado em claro. Mesmo
    // padrão de Lead.nome/Mentorado.telefone — ver V28.
    @Column(nullable = false, columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(descricao, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
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

    // Gap 1 (raio-x, 18/07/2026): quanto já foi pago/recebido enquanto a conta está PARCIAL.
    // Null enquanto PENDENTE/VENCIDO (nunca recebeu pagamento nenhum); igual a `valor` quando
    // liquidada por completo (PAGO/RECEBIDO) — ver liquidarParcial().
    @Column(name = "valor_pago")
    private BigDecimal valorPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaFinanceira categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_id")
    private LancamentoFinanceiro lancamento;

    // Change request 17/07/2026 ("evento rastreado no financeiro") — nullable: a maioria das
    // contas não é ligada a evento nenhum, mesmo critério de categoria. Quando liquidada, o
    // Lancamento gerado automaticamente herda este evento (ver ContaPagarReceberService.liquidar).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    protected ContaPagarReceber() {
    }

    public ContaPagarReceber(TipoConta tipo, String descricao, BigDecimal valor, LocalDate dataVencimento,
                              CategoriaFinanceira categoria) {
        this(tipo, descricao, valor, dataVencimento, categoria, null);
    }

    public ContaPagarReceber(TipoConta tipo, String descricao, BigDecimal valor, LocalDate dataVencimento,
                              CategoriaFinanceira categoria, Evento evento) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.categoria = categoria;
        this.evento = evento;
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

    /** Gap 1 (raio-x, 18/07/2026) — pagamento parcial, acumulativo: pode ser chamado várias vezes
     * (ex.: cliente paga em pedaços fora de um parcelamento estruturado). Se o total acumulado
     * cobrir o valor da conta, liquida por completo (mesmo destino de {@link #liquidar}) — não dá
     * pra ficar PARCIAL tendo pago 100%. Não gera {@link LancamentoFinanceiro}: diferente da
     * liquidação total, um pagamento parcial não corresponde ao valor cheio da conta, e ainda não
     * há decisão de produto sobre lançar cada parcela avulsa (ver nota no Blueprint). */
    public void liquidarParcial(BigDecimal valorPagoAdicional, LocalDate dataPagamento) {
        if (status == StatusConta.PAGO || status == StatusConta.RECEBIDO) {
            throw new IllegalStateException("Esta conta já foi liquidada.");
        }
        if (valorPagoAdicional == null || valorPagoAdicional.signum() <= 0) {
            throw new IllegalArgumentException("Valor pago deve ser positivo.");
        }
        BigDecimal totalPago = (this.valorPago == null ? BigDecimal.ZERO : this.valorPago).add(valorPagoAdicional);
        if (totalPago.compareTo(valor) > 0) {
            throw new IllegalArgumentException("Valor pago não pode ultrapassar o valor da conta.");
        }
        this.dataPagamento = dataPagamento;
        if (totalPago.compareTo(valor) == 0) {
            this.valorPago = totalPago;
            this.status = tipo == TipoConta.A_PAGAR ? StatusConta.PAGO : StatusConta.RECEBIDO;
        } else {
            this.valorPago = totalPago;
            this.status = StatusConta.PARCIAL;
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

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public LancamentoFinanceiro getLancamento() {
        return lancamento;
    }

    public CategoriaFinanceira getCategoria() {
        return categoria;
    }

    public Evento getEvento() {
        return evento;
    }
}
