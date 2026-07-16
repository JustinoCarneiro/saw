package com.sawhub.hub.loja;

import com.sawhub.hub.loja.dto.AtualizarProdutoRequest;
import com.sawhub.hub.loja.dto.CriarProdutoRequest;
import com.sawhub.hub.loja.dto.ProdutoResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import com.sawhub.hub.common.dto.ImportResultResponse;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** H8.1 — CRUD do catálogo da Loja. `Modulo.COMERCIAL` é suposição explícita do Blueprint M14
 * (CLAUDE.md não define qual área da SAW cuida da Loja) — ver ROADMAP.md. */
@RestController
@RequestMapping("/api/v1/admin/produtos")
@RequiresModulo(Modulo.COMERCIAL)
public class ProdutoController {

    private final ProdutoService produtoService;
    private final ProdutoCsvService produtoCsvService;

    public ProdutoController(ProdutoService produtoService, ProdutoCsvService produtoCsvService) {
        this.produtoService = produtoService;
        this.produtoCsvService = produtoCsvService;
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) CategoriaProduto categoria,
                                               @RequestParam(required = false) Boolean publicado,
                                               @RequestParam(required = false) Boolean destaque,
                                               @RequestParam(required = false) String busca) {
        String csv = produtoCsvService.exportar(categoria, publicado, destaque, busca);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"produtos.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importarCsv(@RequestParam("arquivo") MultipartFile arquivo) {
        ImportResultResponse response = produtoCsvService.importar(arquivo);
        if (!response.erros().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
