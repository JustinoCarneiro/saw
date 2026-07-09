package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.security.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "mentorado")
public class Mentorado extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private String nome;

    private String negocio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano = Plano.GRATUITO;

    @Column(name = "crescimento_faturamento_pct", nullable = false)
    private BigDecimal crescimentoFaturamentoPct = BigDecimal.ZERO;

    @Column(name = "ferramentas_concluidas", nullable = false)
    private Integer ferramentasConcluidas = 0;

    @Column(name = "ferramentas_total", nullable = false)
    private Integer ferramentasTotal = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMentorado status = StatusMentorado.ATIVO;

    protected Mentorado() {
    }

    public Mentorado(Usuario usuario, String nome, String negocio, Plano plano,
                      BigDecimal crescimentoFaturamentoPct, Integer ferramentasConcluidas, Integer ferramentasTotal) {
        this.usuario = usuario;
        this.nome = nome;
        this.negocio = negocio;
        this.plano = plano;
        this.crescimentoFaturamentoPct = crescimentoFaturamentoPct;
        this.ferramentasConcluidas = ferramentasConcluidas;
        this.ferramentasTotal = ferramentasTotal;
        this.status = StatusMentorado.ATIVO;
    }

    /** H11.1 — edição administrativa (nome, negócio, plano); status muda por {@link #ativar()}/{@link #desativar()}. */
    public void atualizar(String nome, String negocio, Plano plano) {
        this.nome = nome;
        this.negocio = negocio;
        this.plano = plano;
    }

    public void ativar() {
        this.status = StatusMentorado.ATIVO;
    }

    public void desativar() {
        this.status = StatusMentorado.INATIVO;
    }

    public StatusMentorado getStatus() {
        return status;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNome() {
        return nome;
    }

    public String getNegocio() {
        return negocio;
    }

    public Plano getPlano() {
        return plano;
    }

    public BigDecimal getCrescimentoFaturamentoPct() {
        return crescimentoFaturamentoPct;
    }

    public Integer getFerramentasConcluidas() {
        return ferramentasConcluidas;
    }

    public Integer getFerramentasTotal() {
        return ferramentasTotal;
    }
}
