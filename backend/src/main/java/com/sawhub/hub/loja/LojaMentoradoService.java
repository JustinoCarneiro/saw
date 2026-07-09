package com.sawhub.hub.loja;

import com.sawhub.hub.loja.dto.CarrinhoResponse;
import com.sawhub.hub.loja.dto.CheckoutResponse;
import com.sawhub.hub.loja.dto.PedidoMentoradoResponse;
import com.sawhub.hub.loja.dto.ProdutoCatalogoResponse;
import com.sawhub.hub.loja.pagamento.MercadoPagoGatewayService;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** H8.1-H8.3 (M14) — catálogo, carrinho e início de checkout, mesmo padrão de isolamento por
 * tenant do M08-M13: o id do Mentorado nunca vem de parâmetro, só do usuário autenticado. */
@Service
public class LojaMentoradoService {

    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final MentoradoRepository mentoradoRepository;
    private final MercadoPagoGatewayService gateway;
    private final String frontendBaseUrl;
    private final String backendBaseUrl;

    public LojaMentoradoService(ProdutoRepository produtoRepository, PedidoRepository pedidoRepository,
                                 MentoradoRepository mentoradoRepository, MercadoPagoGatewayService gateway,
                                 @Value("${sawhub.pagamento.frontend-base-url}") String frontendBaseUrl,
                                 @Value("${sawhub.pagamento.backend-base-url}") String backendBaseUrl) {
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.mentoradoRepository = mentoradoRepository;
        this.gateway = gateway;
        this.frontendBaseUrl = frontendBaseUrl;
        this.backendBaseUrl = backendBaseUrl;
    }

    public List<ProdutoCatalogoResponse> listarCatalogo(CategoriaProduto categoria, Boolean destaque, String busca) {
        return produtoRepository.buscarComFiltro(categoria, true, destaque, busca).stream()
                .map(ProdutoCatalogoResponse::from).toList();
    }

    public CarrinhoResponse buscarCarrinho(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        return pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)
                .map(CarrinhoResponse::from)
                .orElse(CarrinhoResponse.vazio());
    }

    @Transactional
    public CarrinhoResponse adicionarItem(UUID usuarioId, UUID produtoId, int quantidade) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        Produto produto = produtoRepository.findById(produtoId)
                .filter(Produto::isPublicado)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado."));
        Pedido carrinho = pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)
                .orElseGet(() -> new Pedido(mentorado));
        carrinho.adicionarItem(produto, quantidade);
        pedidoRepository.save(carrinho);
        return CarrinhoResponse.from(carrinho);
    }

    @Transactional
    public CarrinhoResponse atualizarQuantidade(UUID usuarioId, UUID itemId, int quantidade) {
        Pedido carrinho = buscarCarrinhoDoMentorado(usuarioId);
        carrinho.atualizarQuantidadeItem(itemId, quantidade);
        pedidoRepository.save(carrinho);
        return CarrinhoResponse.from(carrinho);
    }

    @Transactional
    public CarrinhoResponse removerItem(UUID usuarioId, UUID itemId) {
        Pedido carrinho = buscarCarrinhoDoMentorado(usuarioId);
        carrinho.removerItem(itemId);
        pedidoRepository.save(carrinho);
        return CarrinhoResponse.from(carrinho);
    }

    @Transactional
    public CheckoutResponse checkout(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        // Aceita retomar um pedido AGUARDANDO_PAGAMENTO (retry após recusa, "carrinho
        // preservado" de H8.3 — ver Pedido.iniciarCheckout()), não só CARRINHO.
        Pedido carrinho = pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)
                .or(() -> pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.AGUARDANDO_PAGAMENTO))
                .orElseThrow(() -> new NoSuchElementException("Carrinho não encontrado."));
        if (carrinho.getItens().isEmpty()) {
            throw new IllegalStateException("Carrinho vazio.");
        }
        boolean algumIndisponivel = carrinho.getItens().stream().anyMatch(i -> !i.getProduto().isPublicado());
        if (algumIndisponivel) {
            throw new IllegalStateException("Um ou mais itens do carrinho não estão mais disponíveis.");
        }

        var preferencia = gateway.criarPreferencia(carrinho,
                frontendBaseUrl + "/mentorado/loja/pedidos?status=sucesso",
                frontendBaseUrl + "/mentorado/loja/carrinho?status=falha",
                frontendBaseUrl + "/mentorado/loja/pedidos?status=pendente",
                backendBaseUrl + "/api/v1/webhooks/mercadopago");

        // iniciarCheckout() só transiciona o estado DEPOIS da chamada ao gateway ter sucesso —
        // uma falha de rede/credencial nunca deixa o Pedido preso em AGUARDANDO_PAGAMENTO sem
        // um checkout de verdade por trás.
        carrinho.iniciarCheckout(preferencia.preferenceId());
        pedidoRepository.save(carrinho);
        return new CheckoutResponse(preferencia.initPoint());
    }

    public List<PedidoMentoradoResponse> listarPedidos(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        return pedidoRepository.buscarHistorico(mentorado.getId(), StatusPedido.CARRINHO).stream()
                .map(PedidoMentoradoResponse::from).toList();
    }

    private Pedido buscarCarrinhoDoMentorado(UUID usuarioId) {
        Mentorado mentorado = resolverMentorado(usuarioId);
        return pedidoRepository.buscarPorMentoradoEStatus(mentorado.getId(), StatusPedido.CARRINHO)
                .orElseThrow(() -> new NoSuchElementException("Carrinho não encontrado."));
    }

    private Mentorado resolverMentorado(UUID usuarioId) {
        return mentoradoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Mentorado não encontrado para o usuário autenticado."));
    }
}
