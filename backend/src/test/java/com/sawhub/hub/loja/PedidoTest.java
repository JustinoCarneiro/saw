package com.sawhub.hub.loja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** H8.2-H8.4 — máquina de estado e matemática do carrinho, testado direto na entidade (sem
 * mocks): a lógica de negócio mora aqui, não no service (mesmo padrão de Mentoria/Ata/Evento). */
class PedidoTest {

    private static Produto produto(String titulo, String preco) {
        Produto p = new Produto(titulo, "desc", CategoriaProduto.PLANILHA, new BigDecimal(preco), null, null, false,
                "https://cdn.sawhub.com.br/x.zip", null);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        return p;
    }

    @Test
    void adicionarItemNovoCriaLinhaComPrecoSnapshotDoMomento() {
        Pedido pedido = new Pedido(null);
        Produto produto = produto("Planilha", "100.00");

        pedido.adicionarItem(produto, 2);

        assertThat(pedido.getItens()).hasSize(1);
        assertThat(pedido.getItens().get(0).getQuantidade()).isEqualTo(2);
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void adicionarMesmoProdutoDeNovoSomaQuantidadeSemDuplicarLinha() {
        Pedido pedido = new Pedido(null);
        Produto produto = produto("Planilha", "50.00");

        pedido.adicionarItem(produto, 1);
        pedido.adicionarItem(produto, 2);

        assertThat(pedido.getItens()).hasSize(1);
        assertThat(pedido.getItens().get(0).getQuantidade()).isEqualTo(3);
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void precoTravadoNoMomentoDoAddNaoMudaSeProdutoMudarDepois() {
        Pedido pedido = new Pedido(null);
        Produto produto = produto("Planilha", "100.00");
        pedido.adicionarItem(produto, 1);

        produto.atualizar("Planilha", "desc", CategoriaProduto.PLANILHA, new BigDecimal("500.00"), null, null, false,
                "https://cdn.sawhub.com.br/x.zip", null);

        assertThat(pedido.getValorTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void removerItemRecalculaTotal() {
        Pedido pedido = new Pedido(null);
        Produto a = produto("A", "30.00");
        Produto b = produto("B", "20.00");
        pedido.adicionarItem(a, 1);
        pedido.adicionarItem(b, 1);
        // ItemPedido.id só é gerado de verdade na persistência (@GeneratedValue) — num teste
        // puro de entidade (sem JPA), precisa ser atribuído manualmente pra buscarItem() achar a linha.
        UUID itemId = UUID.randomUUID();
        ReflectionTestUtils.setField(pedido.getItens().get(0), "id", itemId);

        pedido.removerItem(itemId);

        assertThat(pedido.getItens()).hasSize(1);
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void mutarItemForaDeCarrinhoLancaErro() {
        Pedido pedido = new Pedido(null);
        pedido.adicionarItem(produto("A", "10.00"), 1);
        pedido.iniciarCheckout("pref-123");

        assertThatThrownBy(() -> pedido.adicionarItem(produto("B", "10.00"), 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void iniciarCheckoutComCarrinhoVazioLancaErro() {
        Pedido pedido = new Pedido(null);
        assertThatThrownBy(() -> pedido.iniciarCheckout("pref-123")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void iniciarCheckoutDeNovoAPartirDeAguardandoPagamentoFuncionaRetry() {
        Pedido pedido = new Pedido(null);
        pedido.adicionarItem(produto("A", "10.00"), 1);
        pedido.iniciarCheckout("pref-1");

        pedido.iniciarCheckout("pref-2");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.getReferenciaGateway()).isEqualTo("pref-2");
    }

    @Test
    void confirmarPagamentoForaDeAguardandoPagamentoLancaErro() {
        Pedido pedido = new Pedido(null);
        assertThatThrownBy(pedido::confirmarPagamento).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fluxoCompletoAteLiberado() {
        Pedido pedido = new Pedido(null);
        pedido.adicionarItem(produto("A", "10.00"), 1);
        pedido.iniciarCheckout("pref-1");

        pedido.confirmarPagamento();
        pedido.liberar();

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.LIBERADO);
    }

    @Test
    void liberarForaDePagoLancaErro() {
        Pedido pedido = new Pedido(null);
        assertThatThrownBy(pedido::liberar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelarAPartirDeCarrinhoFunciona() {
        Pedido pedido = new Pedido(null);
        pedido.cancelar();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void cancelarPedidoJaLiberadoLancaErro() {
        Pedido pedido = new Pedido(null);
        pedido.adicionarItem(produto("A", "10.00"), 1);
        pedido.iniciarCheckout("pref-1");
        pedido.confirmarPagamento();
        pedido.liberar();

        assertThatThrownBy(pedido::cancelar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reembolsarSoFuncionaAPartirDePagoOuLiberado() {
        Pedido carrinho = new Pedido(null);
        assertThatThrownBy(carrinho::reembolsar).isInstanceOf(IllegalStateException.class);

        Pedido liberado = new Pedido(null);
        liberado.adicionarItem(produto("A", "10.00"), 1);
        liberado.iniciarCheckout("pref-1");
        liberado.confirmarPagamento();
        liberado.liberar();
        liberado.reembolsar();

        assertThat(liberado.getStatus()).isEqualTo(StatusPedido.REEMBOLSADO);
    }
}
