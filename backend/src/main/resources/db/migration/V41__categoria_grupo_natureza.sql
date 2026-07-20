-- E14 (última pendência) — subcategorias fixo/variável em CategoriaFinanceira (achado no raio-x
-- da planilha real "DRE Financeira Saw", ver docs/reuniao-2026-07-17-atualizacoes.md § "Planilhas
-- reais do Comercial/Financeiro"). `CategoriaFinanceira` (hoje já a granularidade de subcategoria
-- real — ex. "Aluguel", "Mobiliário") ganha dois campos opcionais:
--   grupo    — agrupamento de departamento/linha (ex. "Estrutura", "Pessoas", "Eventos" pras
--              despesas; "Vendas", "Mentoria" pras receitas) — texto livre, não enum: o vocabulário
--              já é diferente entre receita e despesa na planilha real, forçar um enum único
--              obrigaria valores que não fazem sentido pro outro lado.
--   natureza — Fixa/Variável, achado no raio-x: consistente por subcategoria (mesma subcategoria
--              sempre repete o mesmo valor nas linhas reais), não uma escolha livre por lançamento.
-- Nullable os dois — sem seed em massa das 48 subcategorias confirmadas (GrupoDre de cada uma não
-- foi confirmado pelo cliente, não é uma classificação que dá pra inventar); o Admin popula via
-- "+ Nova categoria" conforme migra o plano de contas real pro sistema.
ALTER TABLE categoria_financeira ADD COLUMN grupo VARCHAR(60);
ALTER TABLE categoria_financeira ADD COLUMN natureza VARCHAR(20);
ALTER TABLE categoria_financeira ADD CONSTRAINT chk_categoria_natureza
    CHECK (natureza IS NULL OR natureza IN ('FIXA', 'VARIAVEL'));
