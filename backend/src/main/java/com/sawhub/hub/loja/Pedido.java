package com.sawhub.hub.loja;

import com.sawhub.hub.common.BaseEntity;
import com.sawhub.hub.mentorado.Mentorado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** H8.2-H8.4 (M14) — CLAUDE.md § Máquinas de estado: "Carrinho -> Aguardando pagamento -> Pago ->
 * Liberado, desvios: Cancelado, Reembolsado". Um carrinho ATIVO por mentorado (índice parcial
 * único em pedido(mentorado_id) WHERE status='CARRINHO', ver V11__loja.sql) — mutações de item só
 * fazem sentido em CARRINHO, por isso {@code exigirCarrinho()} guarda todos os métodos de item.
 * Idempotência de webhook (notificação duplicada do gateway) é responsabilidade do SERVICE, não
 * desta entidade — os métodos de transição aqui são estritos de propósito (lançam se chamados do
 * estado errado), o service confere o estado atual antes de chamar. */
@Entity
@Table(name = "pedido")
public class Pedido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentorado_id", nullable = false)
    private Mentorado mentorado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "referencia_gateway")
    private String referenciaGateway;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Mentorado mentorado) {
        this.mentorado = mentorado;
        this.status = StatusPedido.CARRINHO;
    }

    public void adicionarItem(Produto produto, int quantidade) {
        exigirCarrinho();
        ItemPedido existente = itens.stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId()))
                .findFirst().orElse(null);
        int quantidadeFinal = (existente == null ? 0 : existente.getQuantidade()) + quantidade;
        exigirQuantidadePermitida(produto, quantidadeFinal);
        if (existente != null) {
            existente.somarQuantidade(quantidade);
        } else {
            itens.add(new ItemPedido(this, produto, quantidade, produto.getPreco()));
        }
        recalcularTotal();
    }

    public void atualizarQuantidadeItem(UUID itemId, int quantidade) {
        exigirCarrinho();
        ItemPedido item = buscarItem(itemId);
        exigirQuantidadePermitida(item.getProduto(), quantidade);
        item.atualizarQuantidade(quantidade);
        recalcularTotal();
    }

    // Achado de UX (Fase 5): produto digital de licença única (default de todo produto novo, ver
    // Produto.vendaEmAtacado) não pode acumular quantidade > 1 — nem numa chamada só nem somando
    // "adicionar ao carrinho" repetido do mesmo produto (por isso confere a quantidade FINAL, não
    // só o delta recebido). Front já desabilita o "+"/trava em 1 (LojaPage.tsx), isto é o
    // reforço no servidor — mesmo raciocínio do teto de 20 unidades no @Max dos DTOs.
    private static void exigirQuantidadePermitida(Produto produto, int quantidade) {
        if (!produto.isVendaEmAtacado() && quantidade > 1) {
            throw new IllegalArgumentException(
                    "\"" + produto.getTitulo() + "\" só pode ser comprado em unidade única.");
        }
    }

    public void removerItem(UUID itemId) {
        exigirCarrinho();
        itens.remove(buscarItem(itemId));
        recalcularTotal();
    }

    private ItemPedido buscarItem(UUID itemId) {
        return itens.stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Item não encontrado no carrinho."));
    }

    private void recalcularTotal() {
        this.valorTotal = itens.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void exigirCarrinho() {
        if (status != StatusPedido.CARRINHO) {
            throw new IllegalStateException("Só é possível alterar itens enquanto o pedido está em CARRINHO (está em " + status + ").");
        }
    }

    /** CARRINHO -> AGUARDANDO_PAGAMENTO, ou AGUARDANDO_PAGAMENTO -> AGUARDANDO_PAGAMENTO
     * (retry). H8.3 pede que "o carrinho é preservado" quando um pagamento é recusado — em vez
     * de reverter o status (o que arriscaria colidir com o índice único de 1 carrinho ativo por
     * mentorado se um item novo tiver sido adicionado a outro pedido nesse meio-tempo), tentar de
     * novo só regenera a preferência/link de pagamento sobre o MESMO pedido, sem perder itens.
     * Lança se o pedido não tiver itens ou já estiver num estado pós-pagamento/final. */
    public void iniciarCheckout(String referenciaGateway) {
        if (status != StatusPedido.CARRINHO && status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException(
                    "Só é possível iniciar checkout a partir de CARRINHO ou AGUARDANDO_PAGAMENTO (está em " + status + ").");
        }
        if (itens.isEmpty()) {
            throw new IllegalStateException("Carrinho vazio.");
        }
        this.status = StatusPedido.AGUARDANDO_PAGAMENTO;
        this.referenciaGateway = referenciaGateway;
    }

    /** AGUARDANDO_PAGAMENTO -> PAGO. */
    public void confirmarPagamento() {
        if (status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException(
                    "Pedido precisa estar em AGUARDANDO_PAGAMENTO pra confirmar pagamento (está em " + status + ").");
        }
        this.status = StatusPedido.PAGO;
    }

    /** PAGO -> LIBERADO — catálogo 100% digital, transita junto com confirmarPagamento() no
     * mesmo webhook (ver javadoc de StatusPedido). */
    public void liberar() {
        if (status != StatusPedido.PAGO) {
            throw new IllegalStateException("Pedido precisa estar em PAGO pra liberar (está em " + status + ").");
        }
        this.status = StatusPedido.LIBERADO;
    }

    /** Desvio a partir de qualquer estado não-final. */
    public void cancelar() {
        if (status == StatusPedido.LIBERADO || status == StatusPedido.CANCELADO || status == StatusPedido.REEMBOLSADO) {
            throw new IllegalStateException("Pedido já está em um estado final (" + status + ").");
        }
        this.status = StatusPedido.CANCELADO;
    }

    /** Desvio a partir de PAGO ou LIBERADO — estornar antes do pagamento não faz sentido (usar
     * cancelar()). Ação manual do Admin, ver Suposições do Blueprint M14. */
    public void reembolsar() {
        if (status != StatusPedido.PAGO && status != StatusPedido.LIBERADO) {
            throw new IllegalStateException("Só é possível reembolsar um pedido PAGO ou LIBERADO (está em " + status + ").");
        }
        this.status = StatusPedido.REEMBOLSADO;
    }

    public Mentorado getMentorado() {
        return mentorado;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getReferenciaGateway() {
        return referenciaGateway;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }
}
