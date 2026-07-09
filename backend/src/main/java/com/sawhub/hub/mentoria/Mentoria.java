package com.sawhub.hub.mentoria;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.team.Colaborador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** H11.2 + H5.1/H5.2 — máquina de estado (CLAUDE.md):
 * Agendada -&gt; Confirmada -&gt; Realizada (gera ata), com desvio -&gt; Cancelada a partir de
 * Agendada ou Confirmada. M:N com {@link Mentorado} de propósito mesmo pra INDIVIDUAL (1 membro
 * só) — evita duas modelagens paralelas pra solo x grupo (ver ROADMAP.md M06). */
@Entity
@Table(name = "mentoria")
public class Mentoria extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMentoria tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Colaborador mentor;

    @ManyToMany
    @JoinTable(name = "mentoria_mentorado",
            joinColumns = @JoinColumn(name = "mentoria_id"),
            inverseJoinColumns = @JoinColumn(name = "mentorado_id"))
    private Set<Mentorado> mentorados = new HashSet<>();

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Column(name = "duracao_min", nullable = false)
    private Integer duracaoMin;

    @Column(name = "link_online")
    private String linkOnline;

    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMentoria status;

    // H5.2 (M12) — tabela mentoria_material_recomendado já existia desde o V5__mentorias.sql (M06),
    // nunca tinha sido mapeada em JPA nem exposta em endpoint algum (achado ao investigar o schema
    // pro Blueprint do M12, ver ROADMAP.md). Sem cascade: apagar uma Mentoria não deve apagar o
    // Conteudo associado, e vice-versa — só a linha de associação.
    @ManyToMany
    @JoinTable(name = "mentoria_material_recomendado",
            joinColumns = @JoinColumn(name = "mentoria_id"),
            inverseJoinColumns = @JoinColumn(name = "conteudo_id"))
    private Set<Conteudo> materiaisRecomendados = new HashSet<>();

    protected Mentoria() {
    }

    public Mentoria(TipoMentoria tipo, Colaborador mentor, Set<Mentorado> mentorados, Instant dataHora,
                     Integer duracaoMin, String linkOnline, String local) {
        this.tipo = tipo;
        this.mentor = mentor;
        this.mentorados = new HashSet<>(mentorados);
        this.dataHora = dataHora;
        this.duracaoMin = duracaoMin;
        this.linkOnline = linkOnline;
        this.local = local;
        this.status = StatusMentoria.AGENDADA;
    }

    /** Só a partir de AGENDADA. */
    public void confirmar() {
        exigirStatus(StatusMentoria.AGENDADA);
        this.status = StatusMentoria.CONFIRMADA;
    }

    /** Só a partir de CONFIRMADA — dispara a criação da ata (orquestrado no service). */
    public void realizar() {
        exigirStatus(StatusMentoria.CONFIRMADA);
        this.status = StatusMentoria.REALIZADA;
    }

    /** Desvio a partir de AGENDADA ou CONFIRMADA — não faz sentido cancelar o que já aconteceu. */
    public void cancelar() {
        if (status == StatusMentoria.REALIZADA || status == StatusMentoria.CANCELADA) {
            throw new IllegalStateException("Mentoria já está em um estado final (" + status + ").");
        }
        this.status = StatusMentoria.CANCELADA;
    }

    private void exigirStatus(StatusMentoria esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Mentoria precisa estar em " + esperado + " para essa transição (está em " + status + ").");
        }
    }

    /** Substitui a lista inteira (idempotente) — curadoria do Admin, ver
     * {@code MentoriaService#atualizarMateriais}. Cópia defensiva, mesmo padrão do construtor
     * pra {@code mentorados}. */
    public void atualizarMateriaisRecomendados(Set<Conteudo> materiais) {
        this.materiaisRecomendados = new HashSet<>(materiais);
    }

    public TipoMentoria getTipo() {
        return tipo;
    }

    public Colaborador getMentor() {
        return mentor;
    }

    public Set<Mentorado> getMentorados() {
        return mentorados;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public Integer getDuracaoMin() {
        return duracaoMin;
    }

    public String getLinkOnline() {
        return linkOnline;
    }

    public String getLocal() {
        return local;
    }

    public StatusMentoria getStatus() {
        return status;
    }

    public Set<Conteudo> getMateriaisRecomendados() {
        return materiaisRecomendados;
    }
}
