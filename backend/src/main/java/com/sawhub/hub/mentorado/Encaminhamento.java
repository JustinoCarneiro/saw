package com.sawhub.hub.mentorado;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.mentoria.Mentoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A "tarefa" do E4 — encaminhamento gerado após uma mentoria (H4.5, peso 1 ou 2 que alimenta a
 * pontuação ponderada do Painel Consolidado E17/H17.2) ou criado self-service pelo próprio
 * mentorado (M10, achado no mockup — peso sempre 1 nesse caso, ver {@link #Encaminhamento(Mentorado,
 * String, LocalDate, Prioridade, Meta)}, pra não virar vetor de gaming do próprio ranking).
 *
 * <p>M10 trocou o {@code boolean concluido} por {@link StatusTarefa} persistido — os construtores
 * antigos (que recebem {@code boolean concluido}) continuam com a mesma assinatura por fora,
 * convertendo pra status por dentro, então nenhum call site existente (AtaService, DemoDataSeeder)
 * precisou mudar. {@link #isConcluido()} virou método derivado, não getter de campo, então
 * MentoradoDashboardService (M08) também não precisou mudar.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTarefa status = StatusTarefa.PENDENTE;

    // Nullable de propósito (M10): encaminhamentos antigos e os gerados por ata não têm prazo,
    // só as tarefas self-service novas exigem.
    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridade prioridade = Prioridade.MEDIA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_id")
    private Meta meta;

    // Nullable de propósito (M06): encaminhamentos manuais (E4) não nascem de uma mentoria.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentoria_id")
    private Mentoria mentoria;

    protected Encaminhamento() {
    }

    public Encaminhamento(Mentorado mentorado, String titulo, Integer peso, boolean concluido) {
        this(mentorado, titulo, peso, concluido, null);
    }

    /** M06 — encaminhamento materializado a partir de uma sugestão de IA aceita na publicação da ata. */
    public Encaminhamento(Mentorado mentorado, String titulo, Integer peso, boolean concluido, Mentoria mentoria) {
        this.mentorado = mentorado;
        this.titulo = titulo;
        this.peso = peso;
        this.status = concluido ? StatusTarefa.CONCLUIDA : StatusTarefa.PENDENTE;
        this.mentoria = mentoria;
    }

    /** M10 — criação self-service (H4.1-H4.4): peso sempre 1, fixo, nunca vem do mentorado
     * (peso 2 continua exclusivo do fluxo Admin/ata, H4.5) — sem mentoria de origem. */
    public Encaminhamento(Mentorado mentorado, String titulo, LocalDate prazo, Prioridade prioridade, Meta meta) {
        this.mentorado = mentorado;
        this.titulo = titulo;
        this.peso = 1;
        this.status = StatusTarefa.PENDENTE;
        this.prazo = prazo;
        this.prioridade = prioridade == null ? Prioridade.MEDIA : prioridade;
        this.meta = meta;
    }

    /** Título/prazo/prioridade/vínculo a meta — peso nunca é editável (ver construtor self-service).
     * Bloqueado quando já CONCLUIDA, mesmo padrão do {@code Meta.editar()} (M09). */
    public void editar(String titulo, LocalDate prazo, Prioridade prioridade, Meta meta) {
        exigirNaoConcluida();
        this.titulo = titulo;
        this.prazo = prazo;
        this.prioridade = prioridade == null ? Prioridade.MEDIA : prioridade;
        this.meta = meta;
    }

    /** Só a partir de PENDENTE. */
    public void iniciar() {
        exigirStatus(StatusTarefa.PENDENTE);
        this.status = StatusTarefa.EM_ANDAMENTO;
    }

    /** A partir de PENDENTE ou EM_ANDAMENTO (H4.2 não exige passar por Em andamento primeiro). */
    public void concluir() {
        if (status == StatusTarefa.CONCLUIDA) {
            throw new IllegalStateException("Tarefa já está concluída.");
        }
        this.status = StatusTarefa.CONCLUIDA;
    }

    /** Reabre uma tarefa concluída por engano — volta pra PENDENTE. */
    public void reabrir() {
        exigirStatus(StatusTarefa.CONCLUIDA);
        this.status = StatusTarefa.PENDENTE;
    }

    private void exigirNaoConcluida() {
        if (status == StatusTarefa.CONCLUIDA) {
            throw new IllegalStateException("Tarefa já está concluída — não é mais editável.");
        }
    }

    private void exigirStatus(StatusTarefa esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Tarefa precisa estar em " + esperado + " para essa transição (está em " + status + ").");
        }
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public Mentoria getMentoria() {
        return mentoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public Integer getPeso() {
        return peso;
    }

    public StatusTarefa getStatus() {
        return status;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public Meta getMeta() {
        return meta;
    }

    /** Derivado (não é mais um campo persistido) — mantém `MentoradoDashboardService` (M08) e
     * seus testes compilando sem alteração. */
    public boolean isConcluido() {
        return status == StatusTarefa.CONCLUIDA;
    }
}
