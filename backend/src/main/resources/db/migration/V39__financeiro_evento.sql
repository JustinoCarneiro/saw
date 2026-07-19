-- Change request 17/07/2026 ("Receita/despesa de evento rastreada por evento específico,
-- independente do mês do gasto — conecta E13/venda de ingresso ao financeiro"). Nullable: a
-- imensa maioria das contas/lançamentos não é ligada a evento nenhum, mesmo critério de
-- categoria_id em conta_pagar_receber (V2).
ALTER TABLE conta_pagar_receber ADD COLUMN evento_id UUID REFERENCES evento(id);
ALTER TABLE lancamento_financeiro ADD COLUMN evento_id UUID REFERENCES evento(id);
