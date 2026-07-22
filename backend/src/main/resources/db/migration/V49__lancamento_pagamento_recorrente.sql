-- Gap 6 (Pix Recorrente, change request pós-MVP, confirmado 19/07/2026, ver
-- docs/reuniao-2026-07-17-atualizacoes.md) — LancamentoFinanceiro ganha pagamentoRecorrente:
-- só true quando a venda de origem foi paga via FormaPagamento.PIX_RECORRENTE. Passa a entrar
-- no cálculo de MRR (RelatorioFinanceiroService.dashboardFaturamento) ao lado de
-- OrigemReceita.ASSINATURA. Default FALSE cobre todo dado histórico já existente.
ALTER TABLE lancamento_financeiro ADD COLUMN pagamento_recorrente BOOLEAN NOT NULL DEFAULT FALSE;
