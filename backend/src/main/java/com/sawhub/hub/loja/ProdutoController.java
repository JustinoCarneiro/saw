package com.sawhub.hub.loja;

import com.sawhub.hub.loja.dto.AtualizarProdutoRequest;
import com.sawhub.hub.loja.dto.CriarProdutoRequest;
import com.sawhub.hub.loja.dto.ProdutoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H8.1 — CRUD do catálogo da Loja. `Modulo.COMERCIAL` é suposição explícita do Blueprint M14
 * (CLAUDE.md não define qual área da SAW cuida da Loja) — ver ROADMAP.md. */
@RestController
@RequestMapping("/api/v1/admin/produtos")
@RequiresModulo(Modulo.COMERCIAL)
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criar(@Valid @RequestBody CriarProdutoRequest request) {
        return ProdutoResponse.from(produtoService.criar(request));
    }

    @GetMapping
    public List<ProdutoResponse> listar(@RequestParam(required = false) CategoriaProduto categoria,
                                         @RequestParam(required = false) Boolean publicado,
                                         @RequestParam(required = false) Boolean destaque,
                                         @RequestParam(required = false) String busca) {
        return produtoService.listar(categoria, publicado, destaque, busca).stream().map(ProdutoResponse::from).toList();
    }

    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarProdutoRequest request) {
        return ProdutoResponse.from(produtoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/publicar")
    public ProdutoResponse publicar(@PathVariable UUID id) {
        return ProdutoResponse.from(produtoService.publicar(id));
    }

    @PatchMapping("/{id}/despublicar")
    public ProdutoResponse despublicar(@PathVariable UUID id) {
        return ProdutoResponse.from(produtoService.despublicar(id));
    }
}
