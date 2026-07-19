package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.mentorado.Plano;
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

/** H14.1 — receita/despesa lançada, alimenta fluxo de caixa (H14.1), DRE (H14.2) e dashboard
 * de faturamento (H14.3). Máquina de estado: {@link StatusLancamento#PREVISTO} -&gt;
 * {@link StatusLancamento#REALIZADO}. */
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

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_referencia")
    private Plano planoReferencia;

    // Change request 17/07/2026 ("evento rastreado no financeiro") — nullable, mesmo critério de
    // ContaPagarReceber.evento. Setado sobretudo pela liquidação automática de uma conta ligada a
    // evento (ver ContaPagarReceberService.liquidar), não pela criação manual de lançamento.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    protected LancamentoFinanceiro() {
    }

    public LancamentoFinanceiro(TipoLancamento tipo, CategoriaFinanceira categoria, String descricao,
                                 BigDecimal valor, LocalDate dataCompetencia, StatusLancamento status,
                                 Plano planoReferencia) {
        this(tipo, categoria, descricao, valor, dataCompetencia, status, planoReferencia, null);
    }

    public LancamentoFinanceiro(TipoLancamento tipo, CategoriaFinanceira categoria, String descricao,
                                 BigDecimal valor, LocalDate dataCompetencia, StatusLancamento status,
                                 Plano planoReferencia, Evento evento) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.valor = valor;
        this.dataCompetencia = dataCompetencia;
        this.status = status;
        this.planoReferencia = planoReferencia;
        this.evento = evento;
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

    public Plano getPlanoReferencia() {
        return planoReferencia;
    }

    public Evento getEvento() {
        return evento;
    }
}
