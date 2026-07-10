CREATE TABLE password_reset_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario(id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expira_em   TIMESTAMP NOT NULL,
    usado_em    TIMESTAMP,
    criado_em   TIMESTAMP NOT NULL DEFAULT now(),
    versao      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_password_reset_token_usuario ON password_reset_token(usuario_id);
