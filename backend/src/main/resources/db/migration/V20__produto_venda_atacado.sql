-- Achado de UX (Fase 5): permitir quantidade > 1 pra produto digital de licença única (curso,
-- e-book, template) é um erro induzido do usuário. Default FALSE: produtos existentes continuam
-- só-unidade até o Admin marcar explicitamente os que fazem sentido em lote.
ALTER TABLE produto ADD COLUMN venda_em_atacado BOOLEAN NOT NULL DEFAULT false;
