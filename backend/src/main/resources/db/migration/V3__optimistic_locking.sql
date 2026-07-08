-- Lock otimista em toda entidade (achado da revisão de segurança do E14 — dupla-liquidação
-- concorrente de conta_pagar_receber podia gerar lançamento duplicado no DRE).
ALTER TABLE usuario              ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE colaborador          ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE mentorado            ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE encaminhamento       ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE categoria_financeira ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE lancamento_financeiro ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conta_pagar_receber  ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
