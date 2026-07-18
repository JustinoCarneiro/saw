-- M25 — "Formação Profissional" confirmado pelo Marcos (18/07/2026) como categoria própria de
-- ProdutoVenda, mesmo nível de MENTORIA_CONTINUA/MENTORIA_INDIVIDUAL/CONSULTORIA/FORMULA_SAW
-- (resolve a última Pergunta pendente do Blueprint M25). Não vira TipoContrato (Mentorado) —
-- mesmo tratamento de FORMULA_SAW/INGRESSO_EVENTO/PRODUTO_DIGITAL, ver V27.
ALTER TABLE lead DROP CONSTRAINT chk_lead_produto_venda;
ALTER TABLE lead ADD CONSTRAINT chk_lead_produto_venda
    CHECK (produto_venda IS NULL OR produto_venda IN
        ('MENTORIA_CONTINUA','MENTORIA_INDIVIDUAL','CONSULTORIA','FORMULA_SAW','FORMACAO_PROFISSIONAL','INGRESSO_EVENTO','PRODUTO_DIGITAL'));
