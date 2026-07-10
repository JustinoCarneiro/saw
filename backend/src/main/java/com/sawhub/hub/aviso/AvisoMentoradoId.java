package com.sawhub.hub.aviso;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AvisoMentoradoId implements Serializable {

    @Column(name = "mentorado_id", nullable = false)
    private UUID mentoradoId;

    @Column(name = "aviso_id", nullable = false)
    private UUID avisoId;

    protected AvisoMentoradoId() {
    }

    public AvisoMentoradoId(UUID mentoradoId, UUID avisoId) {
        this.mentoradoId = mentoradoId;
        this.avisoId = avisoId;
    }

    public UUID getMentoradoId() {
        return mentoradoId;
    }

    public UUID getAvisoId() {
        return avisoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvisoMentoradoId that = (AvisoMentoradoId) o;
        return Objects.equals(mentoradoId, that.mentoradoId) && Objects.equals(avisoId, that.avisoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentoradoId, avisoId);
    }
}
