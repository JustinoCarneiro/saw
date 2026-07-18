package com.sawhub.hub.comercial;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.evento.Evento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

/** M25 (change request pós-MVP, 17/07/2026) — venda de ingresso de evento pago. Uma linha por
 * ingresso, não por venda — credenciamento é nominal (uma venda de 2 ingressos gera 2 registros,
 * mesmo padrão das planilhas reais "Vendas Eventos"/"CREDENCIAMENTO"). Domínio diferente de
 * {@link com.sawhub.hub.evento.InscricaoEvento} (E7): aquele é gratuito, ligado a Mentorado, sem
 * categoria/preço/credenciamento — este é venda paga, ligado a Lead. */
@Entity
@Table(name = "venda_ingresso")
public class VendaIngresso extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_ingresso", nullable = false)
    private CategoriaIngresso categoriaIngresso;

    // pgcrypto: pode ser pessoa diferente de quem comprou (credenciamento nominal), PII.
    @Column(name = "nome_credenciado", columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(nome_credenciado, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
    private String nomeCredenciado;

    @Column(length = 100)
    private String setor;

    @Column(nullable = false)
    private boolean almoco;

    @Column(name = "check_in", nullable = false)
    private boolean checkIn;

    protected VendaIngresso() {
    }

    public VendaIngresso(Lead lead, Evento evento, CategoriaIngresso categoriaIngresso, String nomeCredenciado,
                          String setor, boolean almoco) {
        this.lead = lead;
        this.evento = evento;
        this.categoriaIngresso = categoriaIngresso;
        this.nomeCredenciado = nomeCredenciado;
        this.setor = setor;
        this.almoco = almoco;
        this.checkIn = false;
    }

    public void marcarCheckIn() {
        this.checkIn = true;
    }

    public Lead getLead() {
        return lead;
    }

    public Evento getEvento() {
        return evento;
    }

    public CategoriaIngresso getCategoriaIngresso() {
        return categoriaIngresso;
    }

    public String getNomeCredenciado() {
        return nomeCredenciado;
    }

    public String getSetor() {
        return setor;
    }

    public boolean isAlmoco() {
        return almoco;
    }

    public boolean isCheckIn() {
        return checkIn;
    }
}
