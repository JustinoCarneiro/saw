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

// H15.6/H15.7 (M20): carteira e conversaoPct eram colunas armazenadas nunca calculadas (só o
// valor que o seeder escrevia) — removidas. Carteira agora é sempre computada por leitura em
// TeamService (contagem de Mentorado distintos via Mentoria.mentor), mesmo raciocínio de nunca
// guardar um dado derivável (XP do M15, atividades recentes do M16).
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

    protected Colaborador() {
    }

    public Colaborador(Usuario usuario, String nome, Area area) {
        this.usuario = usuario;
        this.nome = nome;
        this.area = area;
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

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setArea(Area area) {
        this.area = area;
    }
}
