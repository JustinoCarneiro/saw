-- M11: E6 · Materiais & Dicas do Brayan — lock otimista em conteudo_mentorado (V8), no mesmo
-- padrão de toda entidade do projeto desde o achado da revisão de segurança do E14: sem @Version,
-- dois favoritar/assistir concorrentes (duplo clique, duas abas) podem se sobrescrever
-- silenciosamente em vez de um deles falhar com 409.
ALTER TABLE conteudo_mentorado ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
