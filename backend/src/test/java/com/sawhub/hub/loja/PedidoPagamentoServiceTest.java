package com.sawhub.hub.loja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.CategoriaFinanceiraRepository;
import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.LancamentoFinanceiroRepository;
import com.sawhub.hub.financeiro.OrigemReceita;
import com.sawhub.hub.financeiro.TipoLancamento;
import com.sawhub.hub.loja.pagamento.MercadoPagoGatewayService;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H8.3-H8.4 — confirmação de pagamento é o caminho mais sensível do módulo (dinheiro de verdade,
 * webhook pode chegar duplicado). RED primeiro: PedidoPagamentoService ainda não existe. */
@ExtendWith(MockitoExtension.class)
class PedidoPagamentoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaFinanceiraRepository;
    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
    @Mock
    private MercadoPagoGatewayService gateway;

    private PedidoPagamentoService service() {
        return new PedidoPagamentoService(pedidoRepository, produtoRepository, categoriaFinanceiraRepository,
                lancamentoFinanceiroRepository, gateway);
    }

    private static Mentorado mentorado() {
        return new Mentorado(null, "Maria", null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0);
    }

    private static Produto produto(UUID id, String preco) {
        Produto p = new Produto("Planilha", "desc", CategoriaProduto.PLANILHA, new BigDecimal(preco), null, null,
                false, "https://cdn.sawhub.com.br/x.zip", null);
        p.publicar();
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private static CategoriaFinanceira categoriaLoja() {
        return new CategoriaFinanceira("Loja SAW", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA);
    }

    @Test
    void pagamentoAprovadoConfirmaELiberaPedidoEIncrementaVendas() {
        UUID pedidoId = UUID.randomUUID();
        Produto produto = produto(UUID.randomUUID(), "100.00");
        Pedido pedido = new Pedido(mentorado());
        ReflectionTestUtils.setField(pedido, "id", pedidoId);
        pedido.adicionarItem(produto, 2);
        pedido.iniciarCheckout("pref-1");

        when(gateway.consultarPagamento("pay-1"))
                .thenReturn(new MercadoPagoGatewayService.PagamentoConsultado("approved", pedidoId.toString()));
        when(pedidoRepository.buscarPorIdComItens(pedidoId)).thenReturn(Optional.of(pedido));
        when(categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.LOJA)).thenReturn(Optional.of(categoriaLoja()));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().processarNotificacao("pay-1");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.LIBERADO);
        assertThat(produto.getVendas()).isEqualTo(2);

        ArgumentCaptor<LancamentoFinanceiro> captor = ArgumentCaptor.forClass(LancamentoFinanceiro.class);
        verify(lancamentoFinanceiroRepository).save(captor.capture());
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("200.00");
        assertThat(captor.getValue().getCategoria().getOrigemReceita()).isEqualTo(OrigemReceita.LOJA);
    }

    @Test
    void notificacaoDuplicadaDeUmPedidoJaLiberadoNaoReprocessa() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido(mentorado());
        ReflectionTestUtils.setField(pedido, "id", pedidoId);
        pedido.adicionarItem(produto(UUID.randomUUID(), "100.00"), 1);
        pedido.iniciarCheckout("pref-1");
        pedido.confirmarPagamento();
        pedido.liberar(); // já processado por uma notificação anterior

        when(gateway.consultarPagamento("pay-1"))
                .thenReturn(new MercadoPagoGatewayService.PagamentoConsultado("approved", pedidoId.toString()));
        when(pedidoRepository.buscarPorIdComItens(pedidoId)).thenReturn(Optional.of(pedido));

        service().processarNotificacao("pay-1");

        verify(lancamentoFinanceiroRepository, never()).save(any());
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void pagamentoRecusadoMantemPedidoEmAguardandoPagamentoSemLancarReceita() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido(mentorado());
        ReflectionTestUtils.setField(pedido, "id", pedidoId);
        pedido.adicionarItem(produto(UUID.randomUUID(), "100.00"), 1);
        pedido.iniciarCheckout("pref-1");

        when(gateway.consultarPagamento("pay-1"))
                .thenReturn(new MercadoPagoGatewayService.PagamentoConsultado("rejected", pedidoId.toString()));
        when(pedidoRepository.buscarPorIdComItens(pedidoId)).thenReturn(Optional.of(pedido));

        service().processarNotificacao("pay-1");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        verify(lancamentoFinanceiroRepository, never()).save(any());
    }

    @Test
    void categoriaLojaAusenteLancaErroClaroEmVezDeSalvarLancamentoOrfao() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido(mentorado());
        ReflectionTestUtils.setField(pedido, "id", pedidoId);
        pedido.adicionarItem(produto(UUID.randomUUID(), "100.00"), 1);
        pedido.iniciarCheckout("pref-1");

        when(gateway.consultarPagamento("pay-1"))
                .thenReturn(new MercadoPagoGatewayService.PagamentoConsultado("approved", pedidoId.toString()));
        when(pedidoRepository.buscarPorIdComItens(pedidoId)).thenReturn(Optional.of(pedido));
        when(categoriaFinanceiraRepository.findByOrigemReceita(OrigemReceita.LOJA)).thenReturn(Optional.empty());
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service().processarNotificacao("pay-1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void notificacaoSemExternalReferenceEIgnoradaSilenciosamente() {
        when(gateway.consultarPagamento("pay-1"))
                .thenReturn(new MercadoPagoGatewayService.PagamentoConsultado("approved", null));

        service().processarNotificacao("pay-1");

        verify(pedidoRepository, never()).buscarPorIdComItens(any());
    }

    @Test
    void pedidoNaoEncontradoLancaErro() {
        UUID pedidoId = UUID.randomUUID();
        when(gateway.consultarPagamento("pay-1"))
                .thenReturn(new MercadoPagoGatewayService.PagamentoConsultado("approved", pedidoId.toString()));
        when(pedidoRepository.buscarPorIdComItens(pedidoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().processarNotificacao("pay-1"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void notificacaoParaPedidoJaCanceladoManualmenteNaoReprocessa() {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido(mentorado());
        ReflectionTestUtils.setField(pedido, "id", pedidoId);
        pedido.cancelar(); // admin cancelou manualmente antes do webhook chegar

        when(gateway.consultarPagamento("pay-1"))
                .thenReturn(new MercadoPagoGatewayService.PagamentoConsultado("approved", pedidoId.toString()));
        when(pedidoRepository.buscarPorIdComItens(pedidoId)).thenReturn(Optional.of(pedido));

        service().processarNotificacao("pay-1");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        verify(lancamentoFinanceiroRepository, never()).save(any());
    }
}
