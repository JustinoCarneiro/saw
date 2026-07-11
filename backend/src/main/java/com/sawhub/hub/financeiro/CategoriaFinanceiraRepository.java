package com.sawhub.hub.financeiro;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, UUID> {

    // M14 (E8 Loja) — usada por PedidoPagamentoService pra achar a categoria "Loja SAW"
    // pré-semeada (ver DemoDataSeeder) e lançar a receita nela quando um Pedido vira PAGO. Mais
    // robusto que buscar por nome (findByNome("Loja SAW") quebraria com um typo/rename).
    Optional<CategoriaFinanceira> findByOrigemReceita(OrigemReceita origemReceita);

    // M21 — import CSV resolve categoria por nome (não há CRUD de categoria, então pedir UUID
    // interno numa planilha externa não faz sentido). Lista, não Optional: "nome" não tem
    // constraint UNIQUE no schema, então o service precisa distinguir "não encontrada" de
    // "ambígua" em vez de escolher uma linha arbitrária.
    List<CategoriaFinanceira> findByNomeIgnoreCase(String nome);
}
