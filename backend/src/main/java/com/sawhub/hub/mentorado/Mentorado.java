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
import java.time.Instant;
import java.time.LocalDate;

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

    @Column(length = 30)
    private String telefone;

    @Column(length = 500)
    private String bio;

    @Column(name = "areas_interesse", length = 300)
    private String areasInteresse;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "vencimento_plano")
    private LocalDate vencimentoPlano;

    // H9.2 — marca a primeira vez que a jornada (XP/conquistas) deste mentorado foi computada
    // depois da migração V18. Distingue "essa conquista já era verdadeira antes de rastrearmos"
    // (backfill, sem data fabricada) de "acabou de acontecer" (data real) — ver
    // PerfilJornadaService.sincronizarConquistas.
    @Column(name = "conquistas_observadas_em")
    private Instant conquistasObservadasEm;

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

    /** H9.1 — autoedição do mentorado: só contato/preferências, nunca identidade/plano (esses são admin-only via {@link #atualizar}). */
    public void atualizarPerfil(String telefone, String bio, String areasInteresse, String fotoUrl) {
        this.telefone = telefone;
        this.bio = bio;
        this.areasInteresse = areasInteresse;
        this.fotoUrl = fotoUrl;
    }

    /** H9.3 — setado pelo Admin junto com o plano (M02/E15); ver Suposição 4 do Blueprint do M15. */
    public void definirVencimentoPlano(LocalDate vencimentoPlano) {
        this.vencimentoPlano = vencimentoPlano;
    }

    public void marcarConquistasObservadas() {
        if (this.conquistasObservadasEm == null) {
            this.conquistasObservadasEm = Instant.now();
        }
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

    public String getTelefone() {
        return telefone;
    }

    public String getBio() {
        return bio;
    }

    public String getAreasInteresse() {
        return areasInteresse;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public LocalDate getVencimentoPlano() {
        return vencimentoPlano;
    }

    public Instant getConquistasObservadasEm() {
        return conquistasObservadasEm;
    }
}
