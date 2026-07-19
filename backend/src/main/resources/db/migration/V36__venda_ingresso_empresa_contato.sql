-- Gap 3 (raio-x em "Vendas Eventos"/"CREDENCIAMENTO", confirmado 19/07/2026) — planilha real
-- guarda empresa/telefone/e-mail do comprador do ingresso, entidade original (M25) só tinha o
-- credenciado. Todos opcionais. nome_empresa não é PII de indivíduo (mesmo critério de
-- mentorado.nome_fantasia, V23); telefone/email entram criptografados (mesmo critério de
-- lead.email/telefone, V19).
ALTER TABLE venda_ingresso ADD COLUMN nome_empresa VARCHAR(255);
ALTER TABLE venda_ingresso ADD COLUMN telefone BYTEA;
ALTER TABLE venda_ingresso ADD COLUMN email BYTEA;
