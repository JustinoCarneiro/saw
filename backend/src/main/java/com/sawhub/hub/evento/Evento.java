package com.sawhub.hub.evento;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/** H11.4 — CRUD admin de eventos. Máquina de estado (CLAUDE.md):
 * Programado -&gt; Ao vivo -&gt; Realizado, com desvio -&gt; Cancelado a partir de Programado ou Ao vivo. */
@Entity
@Table(name = "evento")
public class Evento extends BaseEntity {

    @Column(nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipo;

    private String tema;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    private String local;

    @Column(name = "link_online")
    private String linkOnline;

    private Integer vagas;

    // M13 (H7.2) — contador, não COUNT(*) ao vivo de propósito: mutado só dentro de
    // ocuparVaga()/liberarVaga(), na mesma transação que salva a InscricaoEvento. Como Evento já
    // tem @Version (BaseEntity), duas inscrições concorrentes na última vaga fazem a 2ª save()
    // estourar ObjectOptimisticLockingFailureException (409, já mapeado desde o E14) em vez de
    // estourar a capacidade silenciosamente — um COUNT(*) não teria essa proteção de graça.
    @Column(name = "vagas_ocupadas", nullable = false)
    private Integer vagasOcupadas = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEvento status;

    protected Evento() {
    }

    public Evento(String titulo, TipoEvento tipo, String tema, Instant dataHora, String local,
                  String linkOnline, Integer vagas) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.tema = tema;
        this.dataHora = dataHora;
        this.local = local;
        this.linkOnline = linkOnline;
        this.vagas = vagas;
        this.status = StatusEvento.PROGRAMADO;
    }

    public void atualizar(String titulo, String tema, Instant dataHora, String local, String linkOnline, Integer vagas) {
        this.titulo = titulo;
        this.tema = tema;
        this.dataHora = dataHora;
        this.local = local;
        this.linkOnline = linkOnline;
        this.vagas = vagas;
    }

    public void iniciar() {
        exigirStatus(StatusEvento.PROGRAMADO);
        this.status = StatusEvento.AO_VIVO;
    }

    public void finalizar() {
        exigirStatus(StatusEvento.AO_VIVO);
        this.status = StatusEvento.REALIZADO;
    }

    public void cancelar() {
        if (status == StatusEvento.REALIZADO || status == StatusEvento.CANCELADO) {
            throw new IllegalStateException("Evento já está em um estado final (" + status + ").");
        }
        this.status = StatusEvento.CANCELADO;
    }

    private void exigirStatus(StatusEvento esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Evento precisa estar em " + esperado + " para essa transição (está em " + status + ").");
        }
    }

    /** H7.2 — chamado dentro de EventoMentoradoService.inscrever(), mesma transação que salva a
     * InscricaoEvento. Lança se não há vaga — a checagem de "tem vaga" é invariante do próprio
     * Evento, não do service. */
    public void ocuparVaga() {
        if (vagas != null && vagasOcupadas >= vagas) {
            throw new IllegalStateException("Evento sem vagas disponíveis.");
        }
        this.vagasOcupadas++;
    }

    /** Cancelamento de inscrição libera a vaga pra outro mentorado. */
    public void liberarVaga() {
        this.vagasOcupadas--;
    }

    public String getTitulo() {
        return titulo;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public String getTema() {
        return tema;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public String getLocal() {
        return local;
    }

    public String getLinkOnline() {
        return linkOnline;
    }

    public Integer getVagas() {
        return vagas;
    }

    public Integer getVagasOcupadas() {
        return vagasOcupadas;
    }

    /** null = sem limite de capacidade (mesmo significado de vagas == null). */
    public Integer getVagasDisponiveis() {
        return vagas == null ? null : vagas - vagasOcupadas;
    }

    public StatusEvento getStatus() {
        return status;
    }
}
