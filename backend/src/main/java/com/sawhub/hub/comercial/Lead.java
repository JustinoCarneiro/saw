package com.sawhub.hub.comercial;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.team.Colaborador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** H1.3 + H13.2 — nasce da solicitação pública de acesso (status inicial {@link StatusLead#SOLICITACAO})
 * e progride pelo funil comercial. Máquina de estado (CLAUDE.md): Solicitação -&gt; Em contato -&gt;
 * Proposta -&gt; Fechado, com desvio -&gt; Perdido a partir de qualquer estado não-terminal — cada
 * transição vive num método próprio, impossível pular etapa (ex.: Solicitação direto pra Fechado)
 * de fora desta classe. */
@Entity
@Table(name = "lead")
public class Lead extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    private String telefone;

    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_interesse")
    private Plano planoInteresse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusLead status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Colaborador vendedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_fechado")
    private Plano planoFechado;

    @Column(name = "motivo_perdido")
    private String motivoPerdido;

    @Column(name = "data_fechamento")
    private Instant dataFechamento;

    protected Lead() {
    }

    public Lead(String nome, String email, String telefone, String mensagem, Plano planoInteresse) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.mensagem = mensagem;
        this.planoInteresse = planoInteresse;
        this.status = StatusLead.SOLICITACAO;
    }

    /** Só a partir de SOLICITACAO — primeiro contato do time comercial com o lead. */
    public void moverParaEmContato(Colaborador vendedor) {
        exigirStatus(StatusLead.SOLICITACAO);
        this.status = StatusLead.EM_CONTATO;
        this.vendedor = vendedor;
    }

    /** Só a partir de EM_CONTATO. */
    public void moverParaProposta() {
        exigirStatus(StatusLead.EM_CONTATO);
        this.status = StatusLead.PROPOSTA;
    }

    /** Só a partir de PROPOSTA — venda fechada. Não cria conta de mentorado (isso é do E11,
     * ver ROADMAP.md M05), só registra o resultado comercial. */
    public void fechar(Plano planoFechado) {
        exigirStatus(StatusLead.PROPOSTA);
        this.status = StatusLead.FECHADO;
        this.planoFechado = planoFechado;
        this.dataFechamento = Instant.now();
    }

    /** Desvio a partir de qualquer estado não-terminal (mesmo lead pode ser perdido logo na
     * solicitação, sem nunca ter sido contatado — ex.: spam, fora do público-alvo). */
    public void perder(String motivo) {
        if (status == StatusLead.FECHADO || status == StatusLead.PERDIDO) {
            throw new IllegalStateException("Lead já está em um estado final (" + status + ").");
        }
        this.status = StatusLead.PERDIDO;
        this.motivoPerdido = motivo;
        this.dataFechamento = Instant.now();
    }

    private void exigirStatus(StatusLead esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Lead precisa estar em " + esperado + " para essa transição (está em " + status + ").");
        }
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getMensagem() {
        return mensagem;
    }

    public Plano getPlanoInteresse() {
        return planoInteresse;
    }

    public StatusLead getStatus() {
        return status;
    }

    public Colaborador getVendedor() {
        return vendedor;
    }

    public Plano getPlanoFechado() {
        return planoFechado;
    }

    public String getMotivoPerdido() {
        return motivoPerdido;
    }

    public Instant getDataFechamento() {
        return dataFechamento;
    }
}
