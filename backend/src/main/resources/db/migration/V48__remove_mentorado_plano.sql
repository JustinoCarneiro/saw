-- Fase 7/7 (final) da remoção completa do Plano legado ("não existem planos, mas sim
-- produtos", docs/reuniao-2026-07-17-atualizacoes.md). Última tabela que ainda tinha a coluna —
-- com esta migration, Plano.java (com.sawhub.hub.mentorado.Plano) some do código por completo.

ALTER TABLE mentorado DROP CONSTRAINT IF EXISTS chk_mentorado_plano;
ALTER TABLE mentorado DROP COLUMN plano;
ALTER TABLE mentorado DROP COLUMN vencimento_plano;
-- tipo_contrato continua NULLABLE de propósito: vendas de INGRESSO_EVENTO/PRODUTO_DIGITAL/
-- FORMULA_SAW/FORMACAO_PROFISSIONAL/FICHA_TECNICA_LUCRATIVA (MentoradoAdminService.
-- mapearTipoContrato) legitimamente não têm tipo de contrato de mentoria — forçar NOT NULL
-- exigiria valor fabricado. Dashboard/lista tratam null como bucket "Não informado", não erro.
