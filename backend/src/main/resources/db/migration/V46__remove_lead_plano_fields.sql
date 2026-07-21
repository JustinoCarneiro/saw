-- Fase 5/7 da remoção completa do Plano legado ("não existem planos, mas sim produtos",
-- docs/reuniao-2026-07-17-atualizacoes.md). Lead.planoInteresse/planoFechado eram o modelo legado
-- de tier de assinatura no funil comercial; produtoVenda/tipoContratoFechado (M25/M23) já cobrem
-- "o que foi vendido" de verdade. Lead.fechar(Plano) (o único caminho que escrevia plano_fechado)
-- foi removido do domínio nesta mesma leva — nenhuma linha nova pode nascer com estas colunas
-- preenchidas a partir de agora.

ALTER TABLE lead DROP CONSTRAINT IF EXISTS chk_lead_plano_interesse;
ALTER TABLE lead DROP CONSTRAINT IF EXISTS chk_lead_plano_fechado;
ALTER TABLE lead DROP COLUMN plano_interesse;
ALTER TABLE lead DROP COLUMN plano_fechado;
