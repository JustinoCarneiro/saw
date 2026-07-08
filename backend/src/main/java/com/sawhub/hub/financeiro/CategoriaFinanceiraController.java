package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CategoriaResponse;
import com.sawhub.hub.security.RequiresModulo;
import com.sawhub.hub.team.Modulo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só leitura — populamos as categorias via seed (DemoDataSeeder); CRUD de categoria fica fora
 * do escopo do E14 (as histórias H14.1-H14.4 não pedem cadastro de categoria pelo usuário). */
@RestController
@RequestMapping("/api/v1/admin/financeiro/categorias")
@RequiresModulo(Modulo.FINANCEIRO)
public class CategoriaFinanceiraController {

    private final CategoriaFinanceiraRepository categoriaRepository;

    public CategoriaFinanceiraController(CategoriaFinanceiraRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(CategoriaResponse::from).toList();
    }
}
