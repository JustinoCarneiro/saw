package com.sawhub.hub.evento;

import com.sawhub.hub.mentorado.Mentorado;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

// Não estende BaseEntity de propósito, mesmo motivo do ConteudoMentorado (M11): @EmbeddedId
// composta (mentorado_id, evento_id) é incompatível com o @Id de coluna única de BaseEntity.
// @Version replicado manualmente (achado da revisão de segurança do E14) — protege
// PARTICULARMENTE a corrida de duas inscrições concorrentes na última vaga: ver
// EventoMentoradoService.inscrever(), que muta Evento.vagasOcupadas na mesma transação.
/** H7.2 (M13) — CLAUDE.md § Máquinas de estado: "Disponível -&gt; Inscrito -&gt; Participado
 * (desvio: -&gt; Cancelada)". "Disponível" nunca é persistido (linha só existe a partir de
 * INSCRITA). Cancelar não é permanente — reinscrever() reaproveita a mesma linha (a chave
 * composta impede duplicata). PARTICIPOU é automático: {@link com.sawhub.hub.evento.EventoService#avancarStatus}
 * marca toda inscrição ainda INSCRITA quando o evento vira REALIZADO — não há check-in manual
 * nesta leva. */
@Entity
@Table(name = "inscricao_evento")
public class InscricaoEvento {

    @EmbeddedId
    private InscricaoEventoId id;

    @Version
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mentoradoId")
    private Mentorado mentorado;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventoId")
    private Evento evento;

    @Enumerated(EnumType.STRING)
    private StatusInscricao status;

    protected InscricaoEvento() {}

    public InscricaoEvento(Mentorado mentorado, Evento evento) {
        this.mentorado = mentorado;
        this.evento = evento;
        this.id = new InscricaoEventoId(mentorado.getId(), evento.getId());
        this.status = StatusInscricao.INSCRITA;
    }

    /** Só a partir de CANCELADA — reabre a mesma linha em vez de criar uma nova (a chave
     * composta não permitiria duas linhas pro mesmo par mentorado+evento de qualquer forma). */
    public void reinscrever() {
        if (status != StatusInscricao.CANCELADA) {
            throw new IllegalStateException("Só é possível reinscrever a partir de CANCELADA (está em " + status + ").");
        }
        this.status = StatusInscricao.INSCRITA;
    }

    /** Só a partir de INSCRITA — já cancelada ou já marcada como participação (evento passado)
     * não faz sentido cancelar retroativamente. */
    public void cancelar() {
        if (status != StatusInscricao.INSCRITA) {
            throw new IllegalStateException("Só é possível cancelar a partir de INSCRITA (está em " + status + ").");
        }
        this.status = StatusInscricao.CANCELADA;
    }

    /** Só a partir de INSCRITA — chamado em lote por EventoService quando o evento vira
     * REALIZADO, nunca pelo mentorado diretamente. */
    public void marcarParticipacao() {
        if (status != StatusInscricao.INSCRITA) {
            throw new IllegalStateException("Só é possível marcar participação a partir de INSCRITA (está em " + status + ").");
        }
        this.status = StatusInscricao.PARTICIPOU;
    }

    public InscricaoEventoId getId() {
        return id;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public Evento getEvento() {
        return evento;
    }

    public StatusInscricao getStatus() {
        return status;
    }
}
