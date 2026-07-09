package com.sawhub.hub.meta;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.mentorado.Mentorado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** H3.1–H3.3 (M09) — máquina de estado (CLAUDE.md): {@code Ativa {No prazo | Atenção | Atrasada}
 * -> Concluída}, desvio {@code -> Pausada}. Self-service do mentorado (ver ROADMAP.md M09: nenhuma
 * história cobre curadoria do Admin) — criação/edição/transição sempre escopadas ao próprio
 * mentorado, nunca a um id de request. */
@Entity
@Table(name = "meta")
public class Meta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentorado_id", nullable = false)
    private Mentorado mentorado;

    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @Column(nullable = false)
    private LocalDate prazo;

    @Column(name = "progresso_pct", nullable = false)
    private Integer progressoPct = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMeta status = StatusMeta.ATIVA;

    protected Meta() {
    }

    public Meta(Mentorado mentorado, String titulo, String descricao, LocalDate prazo) {
        this.mentorado = mentorado;
        this.titulo = titulo;
        this.descricao = descricao;
        this.prazo = prazo;
        this.progressoPct = 0;
        this.status = StatusMeta.ATIVA;
    }

    /** Título/descrição/prazo/progresso — bloqueado só quando já CONCLUIDA (meta fechada não se
     * reabre editando campo, existe {@link #reativar()} pra isso a partir de PAUSADA). */
    public void editar(String titulo, String descricao, LocalDate prazo, Integer progressoPct) {
        exigirNaoConcluida();
        this.titulo = titulo;
        this.descricao = descricao;
        this.prazo = prazo;
        this.progressoPct = progressoPct;
    }

    /** Só a partir de ATIVA. */
    public void concluir() {
        exigirStatus(StatusMeta.ATIVA);
        this.status = StatusMeta.CONCLUIDA;
        this.progressoPct = 100;
    }

    /** Desvio, só a partir de ATIVA. */
    public void pausar() {
        exigirStatus(StatusMeta.ATIVA);
        this.status = StatusMeta.PAUSADA;
    }

    /** Só a partir de PAUSADA, volta pra ATIVA. */
    public void reativar() {
        exigirStatus(StatusMeta.PAUSADA);
        this.status = StatusMeta.ATIVA;
    }

    private void exigirNaoConcluida() {
        if (status == StatusMeta.CONCLUIDA) {
            throw new IllegalStateException("Meta já está concluída — não é mais editável.");
        }
    }

    private void exigirStatus(StatusMeta esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Meta precisa estar em " + esperado + " para essa transição (está em " + status + ").");
        }
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public Integer getProgressoPct() {
        return progressoPct;
    }

    public StatusMeta getStatus() {
        return status;
    }
}
