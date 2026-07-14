package com.sawhub.hub.conteudo;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.mentorado.Plano;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** H11.3 — biblioteca de conteúdos curada pelo Admin; acesso do mentorado (E6, futuro) é
 * controlado por {@code planoMinimo}. */
@Entity
@Table(name = "conteudo")
public class Conteudo extends BaseEntity {

    @Column(nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConteudo tipo;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_minimo", nullable = false)
    private Plano planoMinimo = Plano.GRATUITO;

    @Column(nullable = false)
    private boolean publicado = false;

    // H6.3 — duração DECLARADA do material (Admin preenche ao cadastrar), não tempo real de
    // exibição — não há rastreamento de player. Opcional (nullable): materiais antigos/não-vídeo
    // não precisam preencher. Setter separado (não no construtor/atualizar()) pra não mexer na
    // assinatura de nenhum call site já existente (DemoDataSeeder, testes).
    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    protected Conteudo() {
    }

    public Conteudo(String titulo, TipoConteudo tipo, String url, Plano planoMinimo) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.url = url;
        this.planoMinimo = planoMinimo == null ? Plano.GRATUITO : planoMinimo;
    }

    public void atualizar(String titulo, TipoConteudo tipo, String url, Plano planoMinimo) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.url = url;
        this.planoMinimo = planoMinimo == null ? Plano.GRATUITO : planoMinimo;
    }

    public void definirDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public void publicar() {
        this.publicado = true;
    }

    public void despublicar() {
        this.publicado = false;
    }

    public String getTitulo() {
        return titulo;
    }

    public TipoConteudo getTipo() {
        return tipo;
    }

    public String getUrl() {
        return url;
    }

    public Plano getPlanoMinimo() {
        return planoMinimo;
    }

    public boolean isPublicado() {
        return publicado;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }
}
