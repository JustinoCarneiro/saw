package com.sawhub.hub.atividade;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** H10 — registro append-only de marcos de atividade (transições de status que importam pro
 * feed "atividades recentes" do Dashboard Admin), ver AtividadeLogService. {@code tipo} é String
 * livre (não enum) de propósito — mesma convenção já usada em
 * DashboardAdminResponse.AtividadeRecente, cujos 3 tipos originais (MENTORADO_CADASTRADO,
 * EVENTO_CRIADO, CONTEUDO_PUBLICADO) nunca foram persistidos, só derivados na hora; um enum aqui
 * teria que ser reconciliado com aqueles literais mesmo assim. */
@Entity
@Table(name = "atividade_log")
public class AtividadeLog extends BaseEntity {

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String descricao;

    protected AtividadeLog() {
    }

    public AtividadeLog(String tipo, String descricao) {
        this.tipo = tipo;
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }
}
