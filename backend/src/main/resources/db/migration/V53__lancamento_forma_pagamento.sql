-- Pedido do Marcos (22/07/2026, achado na auditoria de clareza — "que dados são relevantes pro
-- financeiro a partir dos filtros que podem ser aplicados na planilha dele?"). A planilha real
-- "DRE Financeira Saw" tem uma coluna "Forma de Pagamento" (Pix/Cartão/Boleto/Hotmart) em toda
-- linha de Despesas/Receitas, que o sistema não capturava em lançamento nenhum. Nullable — dado
-- opcional, nem toda origem de lançamento sabe a forma de pagamento no momento do cadastro.
ALTER TABLE lancamento_financeiro ADD COLUMN forma_pagamento VARCHAR(20);
ALTER TABLE lancamento_financeiro ADD CONSTRAINT chk_lancamento_forma_pagamento
    CHECK (forma_pagamento IS NULL OR forma_pagamento IN ('PIX','PIX_RECORRENTE','CARTAO','BOLETO','HOTMART'));
