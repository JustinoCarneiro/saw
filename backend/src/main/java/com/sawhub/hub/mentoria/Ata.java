package com.sawhub.hub.mentoria;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** H5.2 + diferencial de IA (ROADMAP.md M06) — 1:1 com {@link Mentoria}, nasce vazia quando a
 * mentoria muda pra REALIZADA. Duas máquinas de estado independentes: {@code statusProcessamento}
 * (pipeline de IA: upload -&gt; transcrição -&gt; resumo) e {@code status} (revisão humana:
 * rascunho -&gt; publicada). Publicar nunca depende do processamento ter rodado — o mentor pode
 * sempre escrever a ata manualmente se preferir ou se a IA falhar. */
@Entity
@Table(name = "ata")
public class Ata extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentoria_id", nullable = false, unique = true)
    private Mentoria mentoria;

    @Column(name = "audio_url")
    private String audioUrl;

    // Sem @Lob de propósito: no Postgres isso mapeia String pra `oid` (large object), não `text`
    // (achado ao vivo: Hibernate falhava a validação do schema esperando oid onde a migration
    // criou text). Coluna TEXT já comporta transcrição/resumo longos sem limite de tamanho.
    @Column(columnDefinition = "text")
    private String transcricao;

    @Column(columnDefinition = "text")
    private String resumo;

    // Change request 17/07/2026 ("campo Decisões na ata") — a ata real da operação
    // (PDF "ATA DE REUNIÃO 07/06/2026") tem Participantes/Pauta/Encaminhamentos, mas faltava uma
    // seção própria de Decisões, distinta do resumo livre. Mesmo tratamento de `resumo` (texto
    // simples, sem pgcrypto — mesmo critério já usado nesta entidade).
    @Column(columnDefinition = "text")
    private String decisoes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processamento", nullable = false)
    private StatusProcessamentoAta statusProcessamento = StatusProcessamentoAta.SEM_AUDIO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAta status = StatusAta.RASCUNHO;

    @Column(name = "erro_processamento")
    private String erroProcessamento;

    @Column(name = "publicada_em")
    private Instant publicadaEm;

    protected Ata() {
    }

    public Ata(Mentoria mentoria) {
        this.mentoria = mentoria;
        this.statusProcessamento = StatusProcessamentoAta.SEM_AUDIO;
        this.status = StatusAta.RASCUNHO;
    }

    /** Sobe um novo áudio — permitido de novo mesmo após FALHA (retry) ou CONCLUIDO (regravar). */
    public void iniciarProcessamento(String audioUrl) {
        if (statusProcessamento == StatusProcessamentoAta.PROCESSANDO) {
            throw new IllegalStateException("Já existe um processamento em andamento para esta ata.");
        }
        this.audioUrl = audioUrl;
        this.erroProcessamento = null;
        this.statusProcessamento = StatusProcessamentoAta.PROCESSANDO;
    }

    /** Overload sem decisões — chamador que não conhece o campo novo continua funcionando sem
     * mudar nada (ex.: DemoDataSeeder, testes já existentes). */
    public void concluirProcessamento(String transcricao, String resumo) {
        concluirProcessamento(transcricao, resumo, null);
    }

    public void concluirProcessamento(String transcricao, String resumo, String decisoes) {
        this.transcricao = transcricao;
        this.resumo = resumo;
        this.decisoes = decisoes;
        this.statusProcessamento = StatusProcessamentoAta.CONCLUIDO;
    }

    public void falharProcessamento(String erro) {
        this.erroProcessamento = erro;
        this.statusProcessamento = StatusProcessamentoAta.FALHA;
    }

    /** Edição manual do resumo (revisão humana) — só antes de publicar. */
    public void editarResumo(String resumo) {
        exigirRascunho();
        this.resumo = resumo;
    }

    /** Edição manual das decisões (revisão humana) — mesma janela de {@link #editarResumo}. */
    public void editarDecisoes(String decisoes) {
        exigirRascunho();
        this.decisoes = decisoes;
    }

    public void publicar() {
        exigirRascunho();
        if (statusProcessamento == StatusProcessamentoAta.PROCESSANDO) {
            throw new IllegalStateException("Não é possível publicar enquanto o processamento de IA está em andamento.");
        }
        this.status = StatusAta.PUBLICADA;
        this.publicadaEm = Instant.now();
    }

    // Package-private de propósito (achado baixo da revisão de segurança do M06): AtaService
    // reusa esta checagem em editarSugestao — antes disso, uma sugestão podia ser editada mesmo
    // depois de PUBLICADA, divergindo do que já foi materializado em Encaminhamento na publicação.
    void exigirRascunho() {
        if (status != StatusAta.RASCUNHO) {
            throw new IllegalStateException("Ata já publicada não pode ser alterada.");
        }
    }

    public Mentoria getMentoria() {
        return mentoria;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getTranscricao() {
        return transcricao;
    }

    public String getResumo() {
        return resumo;
    }

    public String getDecisoes() {
        return decisoes;
    }

    public StatusProcessamentoAta getStatusProcessamento() {
        return statusProcessamento;
    }

    public StatusAta getStatus() {
        return status;
    }

    public String getErroProcessamento() {
        return erroProcessamento;
    }

    public Instant getPublicadaEm() {
        return publicadaEm;
    }
}
