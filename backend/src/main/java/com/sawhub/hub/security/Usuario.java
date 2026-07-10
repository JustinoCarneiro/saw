package com.sawhub.hub.security;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    protected Usuario() {
    }

    public Usuario(String email, String passwordHash, Perfil perfil) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.perfil = perfil;
    }

    /** H1.4 (M18) — recebe o hash já codificado (chamador usa {@code PasswordEncoder}), nunca a
     * senha em texto puro; mesmo cuidado do construtor. */
    public void atualizarSenha(String novoPasswordHash) {
        this.passwordHash = novoPasswordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }
}
