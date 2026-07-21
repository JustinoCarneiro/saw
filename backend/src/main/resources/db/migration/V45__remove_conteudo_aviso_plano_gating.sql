-- Change request pós-MVP (reunião 17/07/2026, docs/reuniao-2026-07-17-atualizacoes.md) — "não
-- existem planos, mas sim produtos". Fase 2/7 da remoção completa do Plano legado (Fase 1
-- removeu a aplicação do gating, sem mexer em schema): o campo plano_minimo sai de vez de
-- Conteudo/Aviso. Confirmado com o Marcos em 21/07/2026: todo mentorado ATIVO passa a ver todo
-- conteúdo/aviso publicado, sem segmentação por tier.

ALTER TABLE conteudo DROP CONSTRAINT IF EXISTS chk_conteudo_plano;
ALTER TABLE conteudo DROP COLUMN plano_minimo;

-- aviso.plano_minimo nunca teve CHECK constraint próprio (só chk_aviso_categoria existe).
ALTER TABLE aviso DROP COLUMN plano_minimo;
