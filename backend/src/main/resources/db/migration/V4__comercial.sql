-- E13 · Comercial & Vendas (ROADMAP.md M05)

-- Máquina de estado (CLAUDE.md): Solicitação -> Em contato -> Proposta -> Fechado | desvio: -> Perdido
-- Também fecha H1.3 (E1): nasce da solicitação pública de acesso, status inicial SOLICITACAO.
CREATE TABLE lead (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome              VARCHAR(120) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    telefone          VARCHAR(20),
    mensagem          VARCHAR(500),
    plano_interesse   VARCHAR(20),
    status            VARCHAR(20)  NOT NULL DEFAULT 'SOLICITACAO',
    vendedor_id       UUID REFERENCES colaborador(id),
    plano_fechado     VARCHAR(20),
    motivo_perdido    VARCHAR(255),
    data_fechamento   TIMESTAMP,
    criado_em         TIMESTAMP    NOT NULL DEFAULT now(),
    versao            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_lead_status CHECK (status IN ('SOLICITACAO','EM_CONTATO','PROPOSTA','FECHADO','PERDIDO')),
    CONSTRAINT chk_lead_plano_interesse CHECK (plano_interesse IS NULL OR plano_interesse IN ('GRATUITO','BASICO','ESSENCIAL','PROFISSIONAL')),
    CONSTRAINT chk_lead_plano_fechado CHECK (plano_fechado IS NULL OR plano_fechado IN ('GRATUITO','BASICO','ESSENCIAL','PROFISSIONAL'))
);

CREATE INDEX idx_lead_status ON lead(status);
CREATE INDEX idx_lead_vendedor ON lead(vendedor_id);
CREATE INDEX idx_lead_data_fechamento ON lead(data_fechamento);

-- H13.3: meta por vendedor/período, comparada contra o realizado (COUNT de leads FECHADO no período)
CREATE TABLE meta_comercial (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id       UUID NOT NULL REFERENCES colaborador(id),
    ano               INT  NOT NULL,
    mes               INT  NOT NULL,
    meta_fechamentos  INT  NOT NULL,
    criado_em         TIMESTAMP NOT NULL DEFAULT now(),
    versao            BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_meta_vendedor_periodo UNIQUE (vendedor_id, ano, mes)
);
