CREATE TABLE usuario (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    perfil         VARCHAR(20)  NOT NULL,
    criado_em      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('ADMIN', 'MENTORADO'))
);

CREATE TABLE colaborador (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id     UUID NOT NULL UNIQUE REFERENCES usuario(id),
    nome           VARCHAR(255) NOT NULL,
    area           VARCHAR(30)  NOT NULL,
    carteira       INTEGER,
    conversao_pct  NUMERIC(5,2),
    criado_em      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_colaborador_area CHECK (area IN ('COMERCIAL', 'MARKETING', 'GESTAO_PERFORMANCE', 'FUNDADOR'))
);

CREATE TABLE mentorado (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id                    UUID NOT NULL UNIQUE REFERENCES usuario(id),
    nome                          VARCHAR(255) NOT NULL,
    negocio                       VARCHAR(255),
    plano                         VARCHAR(30) NOT NULL DEFAULT 'GRATUITO',
    crescimento_faturamento_pct   NUMERIC(6,2) NOT NULL DEFAULT 0,
    -- Ferramentas obrigatórias (ficha técnica, DRE, manual da cultura...) ainda não têm lista
    -- fechada com o cliente (ver "Suposições a validar" no spec) — contagem simples por ora,
    -- em vez de um catálogo, até a lista ser confirmada.
    ferramentas_concluidas        INTEGER NOT NULL DEFAULT 0,
    ferramentas_total             INTEGER NOT NULL DEFAULT 0,
    criado_em                     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_mentorado_plano CHECK (plano IN ('GRATUITO', 'BASICO', 'ESSENCIAL', 'PROFISSIONAL'))
);

CREATE TABLE encaminhamento (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentorado_id   UUID NOT NULL REFERENCES mentorado(id),
    titulo         VARCHAR(255) NOT NULL,
    peso           SMALLINT NOT NULL DEFAULT 1,
    concluido      BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_encaminhamento_peso CHECK (peso IN (1, 2))
);

CREATE INDEX idx_encaminhamento_mentorado ON encaminhamento(mentorado_id);
