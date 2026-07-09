package com.sawhub.hub.conteudo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ConteudoMentoradoId implements Serializable {

    @Column(name = "mentorado_id", nullable = false)
    private UUID mentoradoId;

    @Column(name = "conteudo_id", nullable = false)
    private UUID conteudoId;

    protected ConteudoMentoradoId() {}

    public ConteudoMentoradoId(UUID mentoradoId, UUID conteudoId) {
        this.mentoradoId = mentoradoId;
        this.conteudoId = conteudoId;
    }

    public UUID getMentoradoId() {
        return mentoradoId;
    }

    public UUID getConteudoId() {
        return conteudoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConteudoMentoradoId that = (ConteudoMentoradoId) o;
        return Objects.equals(mentoradoId, that.mentoradoId) &&
               Objects.equals(conteudoId, that.conteudoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentoradoId, conteudoId);
    }
}
