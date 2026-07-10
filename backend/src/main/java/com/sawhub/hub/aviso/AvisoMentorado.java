package com.sawhub.hub.aviso;

import com.sawhub.hub.mentorado.Mentorado;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

// Não estende BaseEntity de propósito: @EmbeddedId composta (mentorado_id, aviso_id) é
// incompatível com o @Id UUID auto-gerado de coluna única de BaseEntity — mesmo formato de
// ConteudoMentorado (M11). @Version replicado manualmente pelo mesmo motivo (achado do E14):
// duplo clique em "marcar como lido" concorrente não deve sobrescrever silenciosamente.
@Entity
@Table(name = "aviso_mentorado")
public class AvisoMentorado {

    @EmbeddedId
    private AvisoMentoradoId id;

    @Version
    private Long versao;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mentoradoId")
    private Mentorado mentorado;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("avisoId")
    private Aviso aviso;

    @Column(nullable = false)
    private boolean lido = false;

    @Column(name = "lido_em")
    private Instant lidoEm;

    protected AvisoMentorado() {
    }

    public AvisoMentorado(Mentorado mentorado, Aviso aviso) {
        this.mentorado = mentorado;
        this.aviso = aviso;
        this.id = new AvisoMentoradoId(mentorado.getId(), aviso.getId());
    }

    public AvisoMentoradoId getId() {
        return id;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public Aviso getAviso() {
        return aviso;
    }

    public boolean isLido() {
        return lido;
    }

    public void marcarLido() {
        if (!this.lido) {
            this.lidoEm = Instant.now();
        }
        this.lido = true;
    }

    public Instant getLidoEm() {
        return lidoEm;
    }
}
