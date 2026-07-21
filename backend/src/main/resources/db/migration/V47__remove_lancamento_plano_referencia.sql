-- Fase 6/7 da remoção completa do Plano legado ("não existem planos, mas sim produtos",
-- docs/reuniao-2026-07-17-atualizacoes.md). planoReferencia era write-only desde sempre neste
-- projeto: nenhum cálculo de MRR/DRE/faturamento jamais leu esta coluna (confirmado via grep
-- antes da remoção — só era escrita na criação e nunca consultada em nenhuma query/relatório).
-- Nunca teve CHECK constraint próprio (V2__financeiro.sql).

ALTER TABLE lancamento_financeiro DROP COLUMN plano_referencia;
