-- Gap 8 (raio-x, confirmado 19/07/2026) — CORTESIA saiu do eixo errado (é OrigemVenda, não
-- CategoriaIngresso); o valor real de tipo de ingresso que faltava era BLACK, categoria
-- permanente. Sem UPDATE de dado: nenhuma linha em produção usa CORTESIA (confirmado via grep no
-- código, categoria nunca chegou a ser usada em nenhum seed/fluxo real).
ALTER TABLE venda_ingresso DROP CONSTRAINT chk_venda_ingresso_categoria;
ALTER TABLE venda_ingresso ADD CONSTRAINT chk_venda_ingresso_categoria
    CHECK (categoria_ingresso IN ('ESSENCIAL','VIP','ESPECIAL','BLACK'));
