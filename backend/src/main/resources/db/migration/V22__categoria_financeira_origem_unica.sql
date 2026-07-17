-- Fase 5 (H14.1) — achado do revisor-seguranca: CategoriaFinanceiraService.criar() só checava
-- unicidade de origem_receita em Java (SELECT antes do INSERT), sem constraint no banco. Duas
-- chamadas concorrentes ao novo POST /admin/financeiro/categorias com a mesma origem passam ambas
-- pelo SELECT antes de qualquer INSERT commitar, criando duas categorias com a mesma origem_receita
-- e quebrando PedidoPagamentoService.findByOrigemReceita(LOJA) (espera 0 ou 1 resultado) em produção.
-- origem_receita é nullable (só faz sentido pra tipo RECEITA) — Postgres não considera múltiplos
-- NULL como conflitantes num índice único, então categorias de despesa continuam livres.
CREATE UNIQUE INDEX uq_categoria_financeira_origem_receita ON categoria_financeira (origem_receita);
