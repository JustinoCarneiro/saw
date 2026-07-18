package com.sawhub.hub.atividade;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

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

    // pgcrypto (achado alto do revisor-seguranca, M25): descricao costuma conter nome de lead/
    // mentorado (ex. "Lead fechado: Maria Souza") — PII já protegida na tabela de origem (V19),
    // criptografar aqui também fecha o buraco de reintroduzir esse mesmo dado em claro pro feed
    // "atividades recentes" (Modulo.DASHBOARD, mais amplo que Modulo.COMERCIAL). Ver V28.
    @Column(nullable = false, columnDefinition = "bytea")
    @ColumnTransformer(
            read = "pgp_sym_decrypt(descricao, current_setting('app.encryption_key'))",
            write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))")
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
