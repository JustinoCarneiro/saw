package com.sawhub.hub.loja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.loja.dto.CarrinhoResponse;
import com.sawhub.hub.loja.pagamento.MercadoPagoGatewayService;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LojaMentoradoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;
    @Mock
    private MercadoPagoGatewayService gateway;

    private LojaMentoradoService service() {
        return new LojaMentoradoService(produtoRepository, pedidoRepository, mentoradoRepository, gateway,
                "http://localhost:5173", "http://localhost:8080");
    }

    private static Mentorado mentorado(UUID id) {
        Mentorado m = new Mentorado(null, "Maria", null, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Produto produtoPublicado(UUID id, String preco) {
        Produto p = new Produto("Planilha", "desc", CategoriaProduto.PLANILHA, new BigDecimal(preco), null, null,
                false, "https://cdn.sawhub.com.br/x.zip", null, false);
        p.publicar();
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void adicionarItemCriaCarrinhoNovoQuandoNaoExiste() {
        UUID usuarioId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(produtoPublicado(produtoId, "100.00")));
        when(pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)).thenReturn(Optional.empty());
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CarrinhoResponse resposta = service().adicionarItem(usuarioId, produtoId, 1);

        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void adicionarProdutoNaoPublicadoLanca404() {
        UUID usuarioId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        Produto rascunho = new Produto("X", "desc", CategoriaProduto.EBOOK, new BigDecimal("10.00"), null, null,
                false, "https://cdn.sawhub.com.br/x.zip", null, false);
        ReflectionTestUtils.setField(rascunho, "id", produtoId);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado(UUID.randomUUID())));
        when(produtoRepository.findById(produtoId)).thenReturn(Optional.of(rascunho));

        assertThatThrownBy(() -> service().adicionarItem(usuarioId, produtoId, 1))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void buscarCarrinhoSemPedidoAtivoDevolveVazioSemCriarLinha() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)).thenReturn(Optional.empty());

        CarrinhoResponse resposta = service().buscarCarrinho(usuarioId);

        assertThat(resposta.id()).isNull();
        assertThat(resposta.itens()).isEmpty();
    }

    @Test
    void checkoutComCarrinhoVazioLancaErro() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Pedido carrinhoVazio = new Pedido(mentorado);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        // .or() só avalia o fallback (AGUARDANDO_PAGAMENTO) se o primeiro Optional vier vazio —
        // como CARRINHO já devolve um pedido aqui, não stub o segundo caminho (Mockito estrito
        // reclamaria de stub nunca usado).
        when(pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)).thenReturn(Optional.of(carrinhoVazio));

        assertThatThrownBy(() -> service().checkout(usuarioId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkoutComItemDespublicadoDesdeQueEntrouNoCarrinhoLancaErro() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Produto produto = produtoPublicado(UUID.randomUUID(), "50.00");
        Pedido carrinho = new Pedido(mentorado);
        carrinho.adicionarItem(produto, 1);
        produto.despublicar();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)).thenReturn(Optional.of(carrinho));

        assertThatThrownBy(() -> service().checkout(usuarioId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkoutBemSucedidoTransicionaParaAguardandoPagamentoEDevolveUrl() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Produto produto = produtoPublicado(UUID.randomUUID(), "50.00");
        Pedido carrinho = new Pedido(mentorado);
        carrinho.adicionarItem(produto, 1);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)).thenReturn(Optional.of(carrinho));
        when(gateway.criarPreferencia(any(), any(), any(), any(), any()))
                .thenReturn(new MercadoPagoGatewayService.PreferenciaCriada("https://mp.com/checkout/x", "pref-123"));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service().checkout(usuarioId);

        assertThat(resposta.checkoutUrl()).isEqualTo("https://mp.com/checkout/x");
        assertThat(carrinho.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void listarCatalogoSoAceitaProdutosPublicados() {
        service().listarCatalogo(null, null, null);
        // A garantia real está no argumento passado ao repositório: publicado=true sempre, nunca
        // vindo de parâmetro de request.
        org.mockito.Mockito.verify(produtoRepository).buscarComFiltro(null, true, null, null);
    }

    @Test
    void listarPedidosResolveApenasDoMentoradoAutenticado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(pedidoRepository.buscarHistorico(mentorado.getId(), StatusPedido.CARRINHO)).thenReturn(List.of());

        service().listarPedidos(usuarioId);

        org.mockito.Mockito.verify(pedidoRepository).buscarHistorico(mentorado.getId(), StatusPedido.CARRINHO);
    }
}
