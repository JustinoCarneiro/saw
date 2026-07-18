-- Achado alto do revisor-seguranca (M25, 17/07/2026): LeadService/PedidoAdminService/
-- PedidoPagamentoService/MentoriaService/AtaService/EventoService concatenam nome de lead/
-- mentorado (PII já protegida por pgcrypto na tabela de origem — ver V19) em texto livre NÃO
-- criptografado nestas duas colunas, o que reintroduz em claro exatamente o dado que V19
-- protegeu (backup/dump do banco expõe o nome mesmo com lead.nome/mentorado.nome criptografados).
-- Confirmado antes desta migration (ver relatório de impacto): nenhuma query em nenhum
-- repository do projeto filtra/ordena/faz LIKE em `descricao` de nenhuma das duas tabelas —
-- mesmo critério de elegibilidade já usado em V19.
ALTER TABLE conta_pagar_receber ALTER COLUMN descricao TYPE bytea USING pgp_sym_encrypt(descricao, '${pgcryptoKey}');
ALTER TABLE atividade_log ALTER COLUMN descricao TYPE bytea USING pgp_sym_encrypt(descricao, '${pgcryptoKey}');
