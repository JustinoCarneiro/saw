package com.sawhub.hub.financeiro;

import com.sawhub.hub.financeiro.dto.CriarCategoriaFinanceiraRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fase 5 (H14.1) — reabre o CRUD de categoria que o Blueprint original do E14 deixou fora de
 * escopo (só seed/`DemoDataSeeder`). Sem isso, uma instalação em produção sem
 * {@code SEED_DEMO_DATA=true} nunca teria categoria nenhuma pra lançar nada. */
@Service
public class CategoriaFinanceiraService {

    private final CategoriaFinanceiraRepository categoriaRepository;

    public CategoriaFinanceiraService(CategoriaFinanceiraRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public CategoriaFinanceira criar(CriarCategoriaFinanceiraRequest request) {
        // PedidoPagamentoService/DemoDataSeeder resolvem a categoria "Loja" via
        // findByOrigemReceita(LOJA) esperando 0 ou 1 resultado (Optional) — uma segunda categoria
        // com a mesma origemReceita não-nula quebraria essa busca em runtime (exceção de
        // resultado ambíguo), silenciosamente até alguém tentar comprar algo na Loja. Checagem
        // aqui cobre o caso comum (feedback rápido pro Admin); o índice único
        // uq_categoria_financeira_origem_receita (V22, achado do revisor-seguranca) cobre a janela
        // de corrida entre duas chamadas concorrentes que essa checagem sozinha não pega.
        if (request.origemReceita() != null
                && categoriaRepository.findByOrigemReceita(request.origemReceita()).isPresent()) {
            throw new IllegalStateException(
                    "Já existe uma categoria com a origem de receita \"" + request.origemReceita() + "\".");
        }
        CategoriaFinanceira categoria = new CategoriaFinanceira(
                request.nome(), request.tipo(), request.grupoDre(), request.origemReceita(),
                request.grupo(), request.natureza());
        try {
            // saveAndFlush, não save: o id é gerado em Java (GenerationType.UUID, ver
            // BaseEntity), então um save() simples só enfileira o INSERT — sem flush explícito
            // aqui, a violação do índice único só estouraria no commit da transação, depois deste
            // método já ter retornado, e o catch abaixo nunca veria a exceção.
            return categoriaRepository.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "Já existe uma categoria com a origem de receita \"" + request.origemReceita() + "\".");
        }
    }
}
