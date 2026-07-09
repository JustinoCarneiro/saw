package com.sawhub.hub.mentoria;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Rascunho de encaminhamento gerado pela IA a partir da transcrição — só vira
 * {@link com.sawhub.hub.mentorado.Encaminhamento} de verdade quando a ata é publicada e a
 * sugestão está {@code aceito=true} (revisão humana obrigatória, ver ROADMAP.md M06). */
@Entity
@Table(name = "ata_encaminhamento_sugerido")
public class AtaEncaminhamentoSugerido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ata_id", nullable = false)
    private Ata ata;

    @Column(nullable = false)
    private String titulo;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "peso_sugerido", nullable = false)
    private Integer pesoSugerido = 1;

    @Column(nullable = false)
    private boolean aceito = true;

    protected AtaEncaminhamentoSugerido() {
    }

    public AtaEncaminhamentoSugerido(Ata ata, String titulo, Integer pesoSugerido, boolean aceito) {
        this.ata = ata;
        this.titulo = titulo;
        this.pesoSugerido = pesoSugerido;
        this.aceito = aceito;
    }

    public void editar(String titulo, Integer pesoSugerido, boolean aceito) {
        this.titulo = titulo;
        this.pesoSugerido = pesoSugerido;
        this.aceito = aceito;
    }

    public Ata getAta() {
        return ata;
    }

    public String getTitulo() {
        return titulo;
    }

    public Integer getPesoSugerido() {
        return pesoSugerido;
    }

    public boolean isAceito() {
        return aceito;
    }
}
