package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A "tarefa" do E4 (H4.5) — encaminhamento gerado após uma mentoria, com peso 1 ou 2 que
 * alimenta a pontuação ponderada do Painel Consolidado (E17/H17.2).
 */
@Entity
@Table(name = "encaminhamento")
public class Encaminhamento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentorado_id", nullable = false)
    private Mentorado mentorado;

    @Column(nullable = false)
    private String titulo;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private Integer peso = 1;

    @Column(nullable = false)
    private boolean concluido = false;

    protected Encaminhamento() {
    }

    public Encaminhamento(Mentorado mentorado, String titulo, Integer peso, boolean concluido) {
        this.mentorado = mentorado;
        this.titulo = titulo;
        this.peso = peso;
        this.concluido = concluido;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public String getTitulo() {
        return titulo;
    }

    public Integer getPeso() {
        return peso;
    }

    public boolean isConcluido() {
        return concluido;
    }
}
