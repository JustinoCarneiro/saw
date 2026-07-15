-- Fase 5 · Homologação — pass transversal de pgcrypto (ver CLAUDE.md § Criptografia e a decisão
-- registrada em ROADMAP.md nos módulos M04/M05: "financeiro do mentorado, dados pessoais" em
-- repouso, cobrindo de uma vez só todas as colunas sensíveis que existiam quando o pass rodou.
--
-- Escopo (ver justificativa completa no commit): só colunas NUNCA usadas em WHERE/LIKE/ORDER BY/
-- agregação SQL entram aqui — pgp_sym_encrypt() com chave de sessão não é determinístico entre
-- chamadas, então uma coluna criptografada não pode ser buscada por igualdade/prefixo pelo Postgres
-- sem decriptar linha a linha. Ficam de fora, com justificativa análoga à do DRE (M04):
--   - usuario.email       -> chave de login (findByEmail), recuperação de senha, dedupe de conta.
--   - mentorado.nome      -> busca/filtro do Admin (MentoradoRepository.buscarComFiltro, LIKE).
--   - lancamento_financeiro/conta_pagar_receber.valor -> já excluído no M04 (DRE interno da SAW,
--     não "financeiro do mentorado"; SUM()/GROUP BY nativo do RelatorioFinanceiroService).
--
-- A chave (${pgcryptoKey}, placeholder do Flyway — ver spring.flyway.placeholders no
-- application.yml) precisa ser a MESMA usada em runtime pelo spring.datasource.hikari.
-- connection-init-sql, senão os dados gravados aqui na migração ficam ilegíveis pra aplicação.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- lead: nome, email, telefone, mensagem, motivo_perdido (PII de quem nem é cliente ainda — achado
-- L3 do revisor-seguranca no M05, deferido explicitamente pra este pass).
ALTER TABLE lead ALTER COLUMN nome TYPE bytea USING pgp_sym_encrypt(nome, '${pgcryptoKey}');
ALTER TABLE lead ALTER COLUMN email TYPE bytea USING pgp_sym_encrypt(email, '${pgcryptoKey}');
ALTER TABLE lead ALTER COLUMN telefone TYPE bytea USING pgp_sym_encrypt(telefone, '${pgcryptoKey}');
ALTER TABLE lead ALTER COLUMN mensagem TYPE bytea USING pgp_sym_encrypt(mensagem, '${pgcryptoKey}');
ALTER TABLE lead ALTER COLUMN motivo_perdido TYPE bytea USING pgp_sym_encrypt(motivo_perdido, '${pgcryptoKey}');

-- mentorado: telefone/bio/areas_interesse (dado pessoal) e crescimento_faturamento_pct (é
-- literalmente "financeiro do mentorado" no texto do CLAUDE.md, ao contrário do DRE interno da
-- SAW) — nunca aparece em SUM()/ORDER BY do Postgres, só é ordenado em memória Java
-- (ConsolidatedService, Comparator), por isso pode ser criptografado sem quebrar o ranking do E17.
ALTER TABLE mentorado ALTER COLUMN telefone TYPE bytea USING pgp_sym_encrypt(telefone, '${pgcryptoKey}');
ALTER TABLE mentorado ALTER COLUMN bio TYPE bytea USING pgp_sym_encrypt(bio, '${pgcryptoKey}');
ALTER TABLE mentorado ALTER COLUMN areas_interesse TYPE bytea USING pgp_sym_encrypt(areas_interesse, '${pgcryptoKey}');

ALTER TABLE mentorado ALTER COLUMN crescimento_faturamento_pct DROP DEFAULT;
ALTER TABLE mentorado ALTER COLUMN crescimento_faturamento_pct
    TYPE bytea USING pgp_sym_encrypt(crescimento_faturamento_pct::text, '${pgcryptoKey}');

-- colaborador: nome (dado pessoal do time interno da SAW) — nunca buscado por nome (só listado).
ALTER TABLE colaborador ALTER COLUMN nome TYPE bytea USING pgp_sym_encrypt(nome, '${pgcryptoKey}');
