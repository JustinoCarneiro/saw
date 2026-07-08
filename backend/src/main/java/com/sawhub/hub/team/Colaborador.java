package com.sawhub.hub.team;

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

@Entity
@Table(name = "colaborador")
public class Colaborador extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Area area;

    private Integer carteira;

    @Column(name = "conversao_pct")
    private BigDecimal conversaoPct;

    protected Colaborador() {
    }

    public Colaborador(Usuario usuario, String nome, Area area, Integer carteira, BigDecimal conversaoPct) {
        this.usuario = usuario;
        this.nome = nome;
        this.area = area;
        this.carteira = carteira;
        this.conversaoPct = conversaoPct;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNome() {
        return nome;
    }

    public Area getArea() {
        return area;
    }

    public Integer getCarteira() {
        return carteira;
    }

    public BigDecimal getConversaoPct() {
        return conversaoPct;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setArea(Area area) {
        this.area = area;
    }
}
