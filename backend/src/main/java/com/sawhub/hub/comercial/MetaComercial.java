package com.sawhub.hub.comercial;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.team.Colaborador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** H13.3 — meta de fechamentos por vendedor/período, comparada contra o realizado (COUNT de
 * {@link Lead} em {@link StatusLead#FECHADO} no mesmo período). */
@Entity
@Table(name = "meta_comercial")
public class MetaComercial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Colaborador vendedor;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "meta_fechamentos", nullable = false)
    private Integer metaFechamentos;

    protected MetaComercial() {
    }

    public MetaComercial(Colaborador vendedor, Integer ano, Integer mes, Integer metaFechamentos) {
        this.vendedor = vendedor;
        this.ano = ano;
        this.mes = mes;
        this.metaFechamentos = metaFechamentos;
    }

    public Colaborador getVendedor() {
        return vendedor;
    }

    public Integer getAno() {
        return ano;
    }

    public Integer getMes() {
        return mes;
    }

    public Integer getMetaFechamentos() {
        return metaFechamentos;
    }
}
