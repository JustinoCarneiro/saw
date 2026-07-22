package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Change request pós-MVP (E14, "Transferências Entre Contas"/"Transferências Extraordinárias",
 * reunião 17/07/2026) — movimentação de dinheiro entre contas bancárias da própria SAW (ex.:
 * empréstimo interno Itaú -&gt; Infinity Pay). Não é receita nem despesa (não entra em nenhum
 * {@link GrupoDre}/DRE) — puramente informativo, mesmo conceito encontrado na planilha real "DRE
 * Financeira Saw" (vocabulário de status próprio, "✅ Correspondente", confirma que é conciliação
 * bancária, não fato financeiro — ver docs/reuniao-2026-07-17-atualizacoes.md). */
@Entity
@Table(name = "transferencia_bancaria")
public class TransferenciaBancaria extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_origem_id", nullable = false)
    private ContaBancaria contaOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_destino_id", nullable = false)
    private ContaBancaria contaDestino;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 255)
    private String descricao;

    protected TransferenciaBancaria() {
    }

    public TransferenciaBancaria(ContaBancaria contaOrigem, ContaBancaria contaDestino, BigDecimal valor,
                                  LocalDate data, String descricao) {
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
    }

    public ContaBancaria getContaOrigem() {
        return contaOrigem;
    }

    public ContaBancaria getContaDestino() {
        return contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }
}
