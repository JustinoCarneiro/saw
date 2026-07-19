-- Gap 2 (raio-x, confirmado pelo Marcos 19/07/2026) — "Parceiro" é categoria própria de
-- OrigemVenda, diferente de Cortesia (ver docs/reuniao-2026-07-17-atualizacoes.md).
ALTER TABLE lead DROP CONSTRAINT chk_lead_origem_venda;
ALTER TABLE lead ADD CONSTRAINT chk_lead_origem_venda
    CHECK (origem_venda IS NULL OR origem_venda IN ('DIRETA','HOTMART','CORTESIA','PATROCINIO','PALESTRANTE','PARCEIRO'));
