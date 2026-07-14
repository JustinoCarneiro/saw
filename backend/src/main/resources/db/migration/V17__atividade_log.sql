-- H10 — "atividades recentes" do Dashboard Admin cobria só eventos de CRIAÇÃO (tem timestamp
-- real via criado_em das próprias entidades); marcos de transição de status (mentoria cancelada,
-- pedido pago, lead fechado etc.) não tinham NENHUM timestamp pra ordenar/exibir. Log
-- append-only, escrito nos pontos de transição que importam pro feed (ver AtividadeLogService).
CREATE TABLE atividade_log (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo      VARCHAR(40) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    versao    BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_atividade_log_criado_em ON atividade_log (criado_em DESC);
