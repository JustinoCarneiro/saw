-- M06: E11 · Gestão Admin + E5 · Mentorias & Atas + diferencial de IA (ver ROADMAP.md)

-- Máquina de estado (CLAUDE.md): Agendada -> Confirmada -> Realizada (gera ata) | desvio -> Cancelada
CREATE TABLE mentoria (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo            VARCHAR(20) NOT NULL,
    mentor_id       UUID NOT NULL REFERENCES colaborador(id),
    data_hora       TIMESTAMP NOT NULL,
    duracao_min     INT NOT NULL,
    link_online     VARCHAR(500),
    local           VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'AGENDADA',
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_mentoria_status CHECK (status IN ('AGENDADA','CONFIRMADA','REALIZADA','CANCELADA')),
    CONSTRAINT chk_mentoria_tipo CHECK (tipo IN ('INDIVIDUAL','GRUPO'))
);

CREATE INDEX idx_mentoria_status ON mentoria(status);
CREATE INDEX idx_mentoria_data_hora ON mentoria(data_hora);

-- M:N de propósito mesmo pra INDIVIDUAL (1 linha só) — evita duas modelagens paralelas p/ solo x grupo.
CREATE TABLE mentoria_mentorado (
    mentoria_id     UUID NOT NULL REFERENCES mentoria(id) ON DELETE CASCADE,
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id),
    PRIMARY KEY (mentoria_id, mentorado_id)
);

-- 1:1 com mentoria — nasce (vazia) quando mentoria muda pra REALIZADA.
CREATE TABLE ata (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentoria_id           UUID NOT NULL UNIQUE REFERENCES mentoria(id),
    audio_url             VARCHAR(500),
    transcricao           TEXT,
    resumo                TEXT,
    status_processamento  VARCHAR(20) NOT NULL DEFAULT 'SEM_AUDIO',
    status                VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',
    erro_processamento    VARCHAR(500),
    publicada_em          TIMESTAMP,
    criado_em             TIMESTAMP NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_ata_status_proc CHECK (status_processamento IN ('SEM_AUDIO','PROCESSANDO','CONCLUIDO','FALHA')),
    CONSTRAINT chk_ata_status CHECK (status IN ('RASCUNHO','PUBLICADA'))
);

-- Rascunho gerado pela IA — só materializa em `encaminhamento` de verdade na publicação da ata.
CREATE TABLE ata_encaminhamento_sugerido (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ata_id          UUID NOT NULL REFERENCES ata(id) ON DELETE CASCADE,
    titulo          VARCHAR(255) NOT NULL,
    peso_sugerido   SMALLINT NOT NULL DEFAULT 1,
    aceito          BOOLEAN NOT NULL DEFAULT true,
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_peso_sugerido CHECK (peso_sugerido IN (1,2))
);

-- Nullable de propósito: encaminhamentos já existentes (seed/E4 manual) não têm mentoria de origem.
ALTER TABLE encaminhamento ADD COLUMN mentoria_id UUID REFERENCES mentoria(id);

CREATE TABLE conteudo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo          VARCHAR(255) NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    url             VARCHAR(500) NOT NULL,
    plano_minimo    VARCHAR(20) NOT NULL DEFAULT 'GRATUITO',
    publicado       BOOLEAN NOT NULL DEFAULT false,
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_conteudo_tipo CHECK (tipo IN ('DOCUMENTO','VIDEO','PLANILHA','APRESENTACAO','OUTRO')),
    CONSTRAINT chk_conteudo_plano CHECK (plano_minimo IN ('GRATUITO','BASICO','ESSENCIAL','PROFISSIONAL'))
);

CREATE TABLE mentoria_material_recomendado (
    mentoria_id     UUID NOT NULL REFERENCES mentoria(id) ON DELETE CASCADE,
    conteudo_id     UUID NOT NULL REFERENCES conteudo(id),
    PRIMARY KEY (mentoria_id, conteudo_id)
);

-- Máquina de estado própria (H11.4), independente da de mentoria.
CREATE TABLE evento (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo          VARCHAR(255) NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    tema            VARCHAR(255),
    data_hora       TIMESTAMP NOT NULL,
    local           VARCHAR(255),
    link_online     VARCHAR(500),
    vagas           INT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_evento_status CHECK (status IN ('PROGRAMADO','AO_VIVO','REALIZADO','CANCELADO')),
    CONSTRAINT chk_evento_tipo CHECK (tipo IN ('AO_VIVO','PRESENCIAL'))
);

ALTER TABLE mentorado ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_status CHECK (status IN ('ATIVO','INATIVO'));

-- Fecha a pendência do M05: rastreia de qual lead um mentorado nasceu (H11.1).
ALTER TABLE lead ADD COLUMN mentorado_id UUID REFERENCES mentorado(id);
