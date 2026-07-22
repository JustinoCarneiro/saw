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

/** H14.1 + H14.4 — receita/despesa lançada, alimenta fluxo de caixa, DRE (H14.2) e dashboard de
 * faturamento (H14.3). M26 (change request pós-MVP, 19/07/2026) fundiu o antigo
 * {@code ContaPagarReceber} nesta entidade — "conta a pagar/receber" e "lançamento" eram, na
 * prática, o mesmo fato financeiro em dois estágios (ver ROADMAP.md § "Blueprint (M26)"):
 * {@code dataVencimento} presente = rastreada como conta com prazo; ausente = lançamento direto,
 * sem prazo (ex.: cadastro manual retroativo). Máquina de estado:
 * {@link StatusLancamento#PREVISTO} -&gt; {@link StatusLancamento#PARCIAL} -&gt;
 * {@link StatusLancamento#REALIZADO} (ou desvio PREVISTO -&gt; {@link StatusLancamento#VENCIDO}). */
@Entity
@Table(name = "lancamento_financeiro")
public class LancamentoFinanceiro extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLancamento tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaFinanceira categoria;

    // pgcrypto (lacuna encontrada confirmando a V28): ContaPagarReceberService.liquidar() copia
    // ContaPagarReceber.descricao (já criptografada) pra cá — sem isto, o nome de lead/mentorado
    // reaparece em claro nesta terceira coluna. Ver V29.
    @Column(nullable = false, columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(descricao, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String descricao;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusLancamento status;

    // Change request 17/07/2026 ("evento rastreado no financeiro") — nullable, mesmo critério de
    // ContaPagarReceber.evento. Setado sobretudo pela liquidação automática de uma conta ligada a
    // evento (ver ContaPagarReceberService.liquidar), não pela criação manual de lançamento.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    // M26 — campos absorvidos de ContaPagarReceber. dataVencimento nula = lançamento direto, sem
    // rastreio de prazo (ex.: "Novo lançamento" manual, já PREVISTO/REALIZADO no cadastro).
    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    // Gap 1 (raio-x, 18/07/2026): quanto já foi pago/recebido enquanto o lançamento está PARCIAL.
    // Null enquanto PREVISTO/VENCIDO (nunca recebeu pagamento nenhum); igual a `valor` quando
    // liquidado por completo (REALIZADO) — ver liquidarParcial().
    @Column(name = "valor_pago")
    private BigDecimal valorPago;

    // Gap 6 (Pix Recorrente, confirmado 19/07/2026, ver docs/reuniao-2026-07-17-atualizacoes.md) —
    // true só quando a venda de origem foi paga via FormaPagamento.PIX_RECORRENTE (comercial.Lead,
    // ver LeadService.criarLancamentoValorPagoNoAto). Boolean solto em vez de referenciar
    // comercial.FormaPagamento diretamente: financeiro não pode depender de comercial sem criar
    // ciclo de pacote (comercial já depende de financeiro, ver Javadoc de LeadService). Sinal de
    // receita genuinamente recorrente independente da categoria do produto vendido — usado em
    // RelatorioFinanceiroService.dashboardFaturamento (MRR), ao lado de OrigemReceita.ASSINATURA.
    @Column(name = "pagamento_recorrente", nullable = false)
    private boolean pagamentoRecorrente;

    protected LancamentoFinanceiro() {
    }

    public LancamentoFinanceiro(TipoLancamento tipo, CategoriaFinanceira categoria, String descricao,
                                 BigDecimal valor, LocalDate dataCompetencia, StatusLancamento status) {
        this(tipo, categoria, descricao, valor, dataCompetencia, status, null, null, false);
    }

    public LancamentoFinanceiro(TipoLancamento tipo, CategoriaFinanceira categoria, String descricao,
                                 BigDecimal valor, LocalDate dataCompetencia, StatusLancamento status,
                                 Evento evento) {
        this(tipo, categoria, descricao, valor, dataCompetencia, status, evento, null, false);
    }

    public LancamentoFinanceiro(TipoLancamento tipo, CategoriaFinanceira categoria, String descricao,
                                 BigDecimal valor, LocalDate dataCompetencia, StatusLancamento status,
                                 Evento evento, LocalDate dataVencimento) {
        this(tipo, categoria, descricao, valor, dataCompetencia, status, evento, dataVencimento, false);
    }

    /** Canônico — ganha {@code pagamentoRecorrente} (ver Javadoc do campo). */
    public LancamentoFinanceiro(TipoLancamento tipo, CategoriaFinanceira categoria, String descricao,
                                 BigDecimal valor, LocalDate dataCompetencia, StatusLancamento status,
                                 Evento evento, LocalDate dataVencimento, boolean pagamentoRecorrente) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.valor = valor;
        this.dataCompetencia = dataCompetencia;
        this.status = status;
        this.evento = evento;
        this.dataVencimento = dataVencimento;
        this.pagamentoRecorrente = pagamentoRecorrente;
    }

    /** M26 (absorvido de {@code ContaPagarReceber#liquidar}) — só permitido a partir de
     * PREVISTO/PARCIAL. A data de pagamento também vira a nova {@code dataCompetencia}: o DRE
     * sempre contou o fato financeiro no dia em que foi de fato realizado, não no vencimento
     * original (mesmo comportamento de antes, só que na mesma linha em vez de gerar um segundo
     * registro). */
    public void liquidar(LocalDate dataPagamento) {
        if (status == StatusLancamento.REALIZADO) {
            throw new IllegalStateException("Este lançamento já foi liquidado.");
        }
        this.dataPagamento = dataPagamento;
        this.dataCompetencia = dataPagamento;
        this.valorPago = valor;
        this.status = StatusLancamento.REALIZADO;
    }

    public void marcarVencida() {
        if (status == StatusLancamento.PREVISTO) {
            this.status = StatusLancamento.VENCIDO;
        }
    }

    /** M26 (absorvido de {@code ContaPagarReceber#liquidarParcial}) — pagamento parcial,
     * acumulativo: pode ser chamado várias vezes. Se o total acumulado cobrir o valor do
     * lançamento, liquida por completo (mesmo destino de {@link #liquidar}, inclusive a
     * atualização de {@code dataCompetencia}) — não dá pra ficar PARCIAL tendo pago 100%. */
    public void liquidarParcial(BigDecimal valorPagoAdicional, LocalDate dataPagamento) {
        if (status == StatusLancamento.REALIZADO) {
            throw new IllegalStateException("Este lançamento já foi liquidado.");
        }
        if (valorPagoAdicional == null || valorPagoAdicional.signum() <= 0) {
            throw new IllegalArgumentException("Valor pago deve ser positivo.");
        }
        BigDecimal totalPago = (this.valorPago == null ? BigDecimal.ZERO : this.valorPago).add(valorPagoAdicional);
        if (totalPago.compareTo(valor) > 0) {
            throw new IllegalArgumentException("Valor pago não pode ultrapassar o valor do lançamento.");
        }
        this.dataPagamento = dataPagamento;
        this.valorPago = totalPago;
        if (totalPago.compareTo(valor) == 0) {
            this.dataCompetencia = dataPagamento;
            this.status = StatusLancamento.REALIZADO;
        } else {
            this.status = StatusLancamento.PARCIAL;
        }
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public CategoriaFinanceira getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getDataCompetencia() {
        return dataCompetencia;
    }

    public StatusLancamento getStatus() {
        return status;
    }

    public Evento getEvento() {
        return evento;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public boolean isPagamentoRecorrente() {
        return pagamentoRecorrente;
    }
}
