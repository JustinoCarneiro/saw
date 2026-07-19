-- Gap 6 (raio-x, confirmado 19/07/2026) — Pix Recorrente (assinatura) é distinto de Pix avulso
-- no dado real ("Vendas Aline Melo").
ALTER TABLE lead DROP CONSTRAINT chk_lead_forma_pagamento;
ALTER TABLE lead ADD CONSTRAINT chk_lead_forma_pagamento
    CHECK (forma_pagamento IS NULL OR forma_pagamento IN ('PIX','PIX_RECORRENTE','CARTAO','BOLETO','HOTMART'));
