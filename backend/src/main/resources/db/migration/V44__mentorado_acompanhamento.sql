-- E17/M27 (change request pós-MVP, 19/07/2026, item 3) — "dois eixos de acompanhamento",
-- preenchidos manualmente pelo mentor/time de sucesso (achado no Notion "Padronizações",
-- processo real já é uma "análise pós-check-in"). O status EM_DIA/ATENCAO/ATRASADO calculado em
-- ConsolidatedService continua existindo do mesmo jeito de sempre — estes dois eixos são
-- informação adicional, não substituição. Nullable: nem todo mentorado já foi avaliado.
ALTER TABLE mentorado ADD COLUMN nivel_engajamento VARCHAR(10);
ALTER TABLE mentorado ADD COLUMN risco_churn VARCHAR(10);
ALTER TABLE mentorado ADD COLUMN acompanhamento_avaliado_em TIMESTAMP;

ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_nivel_engajamento
    CHECK (nivel_engajamento IS NULL OR nivel_engajamento IN ('ALTO', 'MEDIO', 'BAIXO'));
ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_risco_churn
    CHECK (risco_churn IS NULL OR risco_churn IN ('NAO', 'ATENCAO', 'ALTO'));
