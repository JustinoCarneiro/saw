-- E14 · Financeiro & DRE (ROADMAP.md M04)

CREATE TABLE categoria_financeira (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(120) NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,
    grupo_dre       VARCHAR(30)  NOT NULL,
    origem_receita  VARCHAR(20),
    criado_em       TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_categoria_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT chk_categoria_grupo_dre CHECK (grupo_dre IN ('RECEITA_BRUTA','DEDUCOES','CUSTOS','DESPESA_OPERACIONAL')),
    CONSTRAINT chk_categoria_origem CHECK (origem_receita IS NULL OR origem_receita IN ('ASSINATURA','LOJA','EVENTO','OUTRA'))
);

-- Máquina de estado: Previsto -> Realizado
CREATE TABLE lancamento_financeiro (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo              VARCHAR(20)  NOT NULL,
    categoria_id      UUID NOT NULL REFERENCES categoria_financeira(id),
    descricao         VARCHAR(255) NOT NULL,
    valor             NUMERIC(12,2) NOT NULL,
    data_competencia  DATE NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PREVISTO',
    plano_referencia  VARCHAR(30),
    criado_em         TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_lancamento_tipo CHECK (tipo IN ('RECEITA','DESPESA')),
    CONSTRAINT chk_lancamento_status CHECK (status IN ('PREVISTO','REALIZADO'))
);

-- Máquina de estado: A pagar/A receber -> Pago/Recebido (ou Vencido)
CREATE TABLE conta_pagar_receber (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo             VARCHAR(20)  NOT NULL,
    descricao        VARCHAR(255) NOT NULL,
    valor            NUMERIC(12,2) NOT NULL,
    data_vencimento  DATE NOT NULL,
    data_pagamento   DATE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    categoria_id     UUID REFERENCES categoria_financeira(id),
    lancamento_id    UUID REFERENCES lancamento_financeiro(id),
    criado_em        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_conta_tipo CHECK (tipo IN ('A_PAGAR','A_RECEBER')),
    CONSTRAINT chk_conta_status CHECK (status IN ('PENDENTE','PAGO','RECEBIDO','VENCIDO'))
);

CREATE INDEX idx_lancamento_competencia ON lancamento_financeiro(data_competencia);
CREATE INDEX idx_lancamento_categoria ON lancamento_financeiro(categoria_id);
CREATE INDEX idx_conta_vencimento ON conta_pagar_receber(data_vencimento);
CREATE INDEX idx_conta_status ON conta_pagar_receber(status);
