package com.sawhub.hub.aviso;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** H12.2 — "criar" já é "publicar", sem rascunho (ver Suposição 3 do Blueprint M17). */
@Entity
@Table(name = "aviso")
public class Aviso extends BaseEntity {

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaAviso categoria;

    protected Aviso() {
    }

    public Aviso(String titulo, String descricao, CategoriaAviso categoria) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
    }

    public void atualizar(String titulo, String descricao, CategoriaAviso categoria) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public CategoriaAviso getCategoria() {
        return categoria;
    }
}
