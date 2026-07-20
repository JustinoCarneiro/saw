package com.sawhub.hub.comercial;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.ColumnTransformer;

/** M25 (change request pós-MVP, 17/07/2026) — parcelamento estruturado de uma venda, não
 * existia em lugar nenhum antes (nem coluna solta, nem tabela — confirmado na pesquisa do
 * Blueprint). Cada parcela gera um {@link LancamentoFinanceiro} RECEITA (ver
 * LeadService.fecharVenda) — reusa a entidade existente do Financeiro em vez de duplicar o
 * conceito de "conta a receber" (M26 fundiu {@code ContaPagarReceber} em
 * {@code LancamentoFinanceiro}, ver ROADMAP.md § "Blueprint (M26)"). */
@Entity
@Table(name = "parcela_venda")
public class ParcelaVenda extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(nullable = false)
    private Integer numero;

    // pgcrypto (mesmo critério do V19/M23): valor de parcela é financeiro do mentorado/lead,
    // nunca usado em WHERE/ORDER BY/SUM nesta leva.
    @Column(nullable = false, columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(valor, current_setting('app.encryption_key'))::numeric",
            write = "pgp_sym_encrypt(?::text, current_setting('app.encryption_key'))")
    private BigDecimal valor;

    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_financeiro_id")
    private LancamentoFinanceiro lancamento;

    protected ParcelaVenda() {
    }

    public ParcelaVenda(Lead lead, Integer numero, BigDecimal valor, LocalDate dataPrevista) {
        this.lead = lead;
        this.numero = numero;
        this.valor = valor;
        this.dataPrevista = dataPrevista;
    }

    public void vincularLancamento(LancamentoFinanceiro lancamento) {
        this.lancamento = lancamento;
    }

    public Lead getLead() {
        return lead;
    }

    public Integer getNumero() {
        return numero;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public LancamentoFinanceiro getLancamento() {
        return lancamento;
    }
}
