package com.sawhub.hub.security;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** H1.4 (M18) — token de uso único pra redefinição de senha. Nunca guarda o valor bruto do
 * token (só o hash, ver {@code PasswordResetService}), mesmo cuidado de nunca guardar senha em
 * texto puro. */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(Usuario usuario, String tokenHash, Instant expiraEm) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public boolean isValido() {
        return usadoEm == null && Instant.now().isBefore(expiraEm);
    }

    public void marcarUsado() {
        this.usadoEm = Instant.now();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getUsadoEm() {
        return usadoEm;
    }
}
