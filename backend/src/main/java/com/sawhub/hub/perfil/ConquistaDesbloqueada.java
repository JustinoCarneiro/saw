package com.sawhub.hub.perfil;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** H9.2 — {@code desbloqueadaEm} nulo significa "já era verdadeira antes da migração V18"
 * (mostrado como "Desde sempre" no frontend, nunca uma data fabricada); não-nulo é uma data real
 * de desbloqueio, gravada em {@link PerfilJornadaService#jornada}. Ver
 * {@code Mentorado.conquistasObservadasEm} pra como essa distinção é decidida. */
@Entity
@Table(name = "conquista_desbloqueada", uniqueConstraints = @UniqueConstraint(columnNames = {"mentorado_id", "codigo"}))
public class ConquistaDesbloqueada extends BaseEntity {

    @Column(name = "mentorado_id", nullable = false)
    private UUID mentoradoId;

    @Column(nullable = false)
    private String codigo;

    @Column(name = "desbloqueada_em")
    private Instant desbloqueadaEm;

    protected ConquistaDesbloqueada() {
    }

    public ConquistaDesbloqueada(UUID mentoradoId, String codigo, Instant desbloqueadaEm) {
        this.mentoradoId = mentoradoId;
        this.codigo = codigo;
        this.desbloqueadaEm = desbloqueadaEm;
    }

    public UUID getMentoradoId() {
        return mentoradoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public Instant getDesbloqueadaEm() {
        return desbloqueadaEm;
    }
}
