package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CategoriaResponse;
import com.sawhub.hub.financeiro.dto.CriarCategoriaFinanceiraRequest;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Fase 5 (H14.1) — cadastro de categoria reaberto (ver {@link CategoriaFinanceiraService}); sem
 * isso, uma instalação sem {@code SEED_DEMO_DATA=true} não tinha como lançar nada no Financeiro. */
@RestController
@RequestMapping("/api/v1/admin/financeiro/categorias")
@RequiresModulo(Modulo.FINANCEIRO)
public class CategoriaFinanceiraController {

    private final CategoriaFinanceiraRepository categoriaRepository;
    private final CategoriaFinanceiraService categoriaService;

    public CategoriaFinanceiraController(CategoriaFinanceiraRepository categoriaRepository,
                                          CategoriaFinanceiraService categoriaService) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(CategoriaResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criar(@Valid @RequestBody CriarCategoriaFinanceiraRequest request) {
        return CategoriaResponse.from(categoriaService.criar(request));
    }
}
