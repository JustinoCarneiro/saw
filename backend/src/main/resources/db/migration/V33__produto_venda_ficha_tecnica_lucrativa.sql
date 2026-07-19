-- Gap 5 (raio-x em "Vendas Aline Melo", confirmado pelo Marcos 19/07/2026) — "Ficha técnica
-- Lucrativa" é categoria própria de ProdutoVenda, mesmo tratamento de FORMULA_SAW/
-- FORMACAO_PROFISSIONAL (ver V27/V30).
ALTER TABLE lead DROP CONSTRAINT chk_lead_produto_venda;
ALTER TABLE lead ADD CONSTRAINT chk_lead_produto_venda
    CHECK (produto_venda IS NULL OR produto_venda IN
        ('MENTORIA_CONTINUA','MENTORIA_INDIVIDUAL','CONSULTORIA','FORMULA_SAW','FORMACAO_PROFISSIONAL',
         'FICHA_TECNICA_LUCRATIVA','INGRESSO_EVENTO','PRODUTO_DIGITAL'));
