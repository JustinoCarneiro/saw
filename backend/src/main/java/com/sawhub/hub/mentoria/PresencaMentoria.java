package com.sawhub.hub.mentoria;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.mentorado.Mentorado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** E17/M27 (change request pós-MVP, 19/07/2026) — presença por mentorado numa mentoria
 * {@link TipoMentoria#GRUPO} (individual já é coberta pelo status da sessão inteira, ver
 * ROADMAP.md § "Blueprint (M27)"). Entidade nova em vez de adicionar atributo ao
 * {@code @ManyToMany} simples de {@code Mentoria.mentorados} — converter esse relacionamento
 * mudaria o mapeamento já existente; aditiva de propósito. */
@Entity
@Table(name = "presenca_mentoria", uniqueConstraints = @UniqueConstraint(columnNames = {"mentoria_id", "mentorado_id"}))
public class PresencaMentoria extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentoria_id", nullable = false)
    private Mentoria mentoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentorado_id", nullable = false)
    private Mentorado mentorado;

    @Column(nullable = false)
    private boolean presente;

    protected PresencaMentoria() {
    }

    public PresencaMentoria(Mentoria mentoria, Mentorado mentorado, boolean presente) {
        this.mentoria = mentoria;
        this.mentorado = mentorado;
        this.presente = presente;
    }

    public void marcar(boolean presente) {
        this.presente = presente;
    }

    public Mentoria getMentoria() {
        return mentoria;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public boolean isPresente() {
        return presente;
    }
}
