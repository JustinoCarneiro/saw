package com.sawhub.hub.conteudo;

import com.sawhub.hub.mentorado.Mentorado;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

// Não estende BaseEntity de propósito: BaseEntity assume @Id UUID auto-gerado de coluna única,
// incompatível com a @EmbeddedId composta (mentorado_id, conteudo_id) desta entidade. O @Version
// é replicado manualmente aqui pelo mesmo motivo de toda entidade do projeto (achado da revisão
// de segurança do E14): sem lock otimista, dois favoritar/assistir concorrentes (ex.: duplo clique,
// duas abas) podem se sobrescrever silenciosamente em vez de uma delas falhar com 409.
@Entity
@Table(name = "conteudo_mentorado")
public class ConteudoMentorado {

    @EmbeddedId
    private ConteudoMentoradoId id;

    @Version
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mentoradoId")
    private Mentorado mentorado;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conteudoId")
    private Conteudo conteudo;

    @Column(nullable = false)
    private boolean favorito = false;

    @Column(nullable = false)
    private boolean assistido = false;

    @Column(name = "data_consumo")
    private Instant dataConsumo;

    protected ConteudoMentorado() {}

    public ConteudoMentorado(Mentorado mentorado, Conteudo conteudo) {
        this.mentorado = mentorado;
        this.conteudo = conteudo;
        this.id = new ConteudoMentoradoId(mentorado.getId(), conteudo.getId());
    }

    public ConteudoMentoradoId getId() {
        return id;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public Conteudo getConteudo() {
        return conteudo;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }

    public boolean isAssistido() {
        return assistido;
    }

    public void setAssistido(boolean assistido) {
        if (!this.assistido && assistido) {
            this.dataConsumo = Instant.now();
        }
        this.assistido = assistido;
    }

    public Instant getDataConsumo() {
        return dataConsumo;
    }
}
