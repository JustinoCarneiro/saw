package com.sawhub.hub.evento;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class InscricaoEventoId implements Serializable {

    @Column(name = "mentorado_id", nullable = false)
    private UUID mentoradoId;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    protected InscricaoEventoId() {}

    public InscricaoEventoId(UUID mentoradoId, UUID eventoId) {
        this.mentoradoId = mentoradoId;
        this.eventoId = eventoId;
    }

    public UUID getMentoradoId() {
        return mentoradoId;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InscricaoEventoId that = (InscricaoEventoId) o;
        return Objects.equals(mentoradoId, that.mentoradoId) &&
               Objects.equals(eventoId, that.eventoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mentoradoId, eventoId);
    }
}
