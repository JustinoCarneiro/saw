package com.sawhub.hub.loja;

import com.sawhub.hub.loja.dto.AdicionarItemCarrinhoRequest;
import com.sawhub.hub.loja.dto.AtualizarQuantidadeItemRequest;
import com.sawhub.hub.loja.dto.CarrinhoResponse;
import com.sawhub.hub.loja.dto.CheckoutResponse;
import com.sawhub.hub.loja.dto.PedidoMentoradoResponse;
import com.sawhub.hub.loja.dto.ProdutoCatalogoResponse;
import com.sawhub.hub.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H8.1-H8.4 (M14) — `hasRole("MENTORADO")` já garantido pelo SecurityConfig
 * (`/api/v1/mentorado/**`); mesmo padrão de isolamento do M08-M13. */
@RestController
@RequestMapping("/api/v1/mentorado/loja")
public class LojaMentoradoController {

    private final LojaMentoradoService lojaMentoradoService;

    public LojaMentoradoController(LojaMentoradoService lojaMentoradoService) {
        this.lojaMentoradoService = lojaMentoradoService;
    }

    @GetMapping("/produtos")
    public List<ProdutoCatalogoResponse> listarCatalogo(@RequestParam(required = false) CategoriaProduto categoria,
                                                          @RequestParam(required = false) Boolean destaque,
                                                          @RequestParam(required = false) String busca) {
        return lojaMentoradoService.listarCatalogo(categoria, destaque, busca);
    }

    @GetMapping("/carrinho")
    public CarrinhoResponse buscarCarrinho(@AuthenticationPrincipal AppUserPrincipal principal) {
        return lojaMentoradoService.buscarCarrinho(principal.getUsuarioId());
    }

    @PostMapping("/carrinho/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public CarrinhoResponse adicionarItem(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @Valid @RequestBody AdicionarItemCarrinhoRequest request) {
        return lojaMentoradoService.adicionarItem(principal.getUsuarioId(), request.produtoId(), request.quantidade());
    }

    @PatchMapping("/carrinho/itens/{itemId}")
    public CarrinhoResponse atualizarQuantidade(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @PathVariable UUID itemId,
                                                 @Valid @RequestBody AtualizarQuantidadeItemRequest request) {
        return lojaMentoradoService.atualizarQuantidade(principal.getUsuarioId(), itemId, request.quantidade());
    }

    @DeleteMapping("/carrinho/itens/{itemId}")
    public CarrinhoResponse removerItem(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID itemId) {
        return lojaMentoradoService.removerItem(principal.getUsuarioId(), itemId);
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@AuthenticationPrincipal AppUserPrincipal principal) {
        return lojaMentoradoService.checkout(principal.getUsuarioId());
    }

    @GetMapping("/pedidos")
    public List<PedidoMentoradoResponse> listarPedidos(@AuthenticationPrincipal AppUserPrincipal principal) {
        return lojaMentoradoService.listarPedidos(principal.getUsuarioId());
    }
}
