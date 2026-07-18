-- M25 — "Fórmula SAW" confirmado pelo Marcos (18/07/2026) como categoria própria de ProdutoVenda,
-- mesmo nível de MENTORIA_CONTINUA/MENTORIA_INDIVIDUAL/CONSULTORIA (resolve a Suposição 1 do
-- Blueprint M25). Não vira TipoContrato (Mentorado) — só ProdutoVenda (Lead) por enquanto; sem
-- confirmação de que "Fórmula SAW" gera um contrato de mentorado com vencimento.
ALTER TABLE lead DROP CONSTRAINT chk_lead_produto_venda;
ALTER TABLE lead ADD CONSTRAINT chk_lead_produto_venda
    CHECK (produto_venda IS NULL OR produto_venda IN
        ('MENTORIA_CONTINUA','MENTORIA_INDIVIDUAL','CONSULTORIA','FORMULA_SAW','INGRESSO_EVENTO','PRODUTO_DIGITAL'));
