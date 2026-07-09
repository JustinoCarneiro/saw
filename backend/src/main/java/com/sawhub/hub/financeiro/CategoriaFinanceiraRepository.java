package com.sawhub.hub.financeiro;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, UUID> {

    // M14 (E8 Loja) — usada por PedidoPagamentoService pra achar a categoria "Loja SAW"
    // pré-semeada (ver DemoDataSeeder) e lançar a receita nela quando um Pedido vira PAGO. Mais
    // robusto que buscar por nome (findByNome("Loja SAW") quebraria com um typo/rename).
    Optional<CategoriaFinanceira> findByOrigemReceita(OrigemReceita origemReceita);
}
