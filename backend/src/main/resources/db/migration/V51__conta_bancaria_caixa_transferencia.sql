-- Change request pós-MVP (E14, "Caixa do mês: Inicial, saldo por banco, Final" +
-- "Transferências Entre Contas", reunião 17/07/2026, ver docs/reuniao-2026-07-17-atualizacoes.md
-- § "Planilhas reais do Comercial/Financeiro"). Três tabelas novas, sem ligação com
-- lancamento_financeiro (a planilha de origem também não amarra lançamento a banco).

CREATE TABLE conta_bancaria (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(80) NOT NULL,
    ativa       BOOLEAN NOT NULL DEFAULT true,
    criado_em   TIMESTAMP NOT NULL DEFAULT now(),
    versao      BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_conta_bancaria_nome UNIQUE (nome)
);

CREATE TABLE posicao_caixa_mensal (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_bancaria_id   UUID NOT NULL REFERENCES conta_bancaria(id),
    ano                 INT NOT NULL,
    mes                 INT NOT NULL,
    saldo_inicial       NUMERIC(12,2) NOT NULL,
    saldo_final         NUMERIC(12,2) NOT NULL,
    criado_em           TIMESTAMP NOT NULL DEFAULT now(),
    versao              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_posicao_caixa_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT uq_posicao_caixa_conta_periodo UNIQUE (conta_bancaria_id, ano, mes)
);

CREATE TABLE transferencia_bancaria (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conta_origem_id     UUID NOT NULL REFERENCES conta_bancaria(id),
    conta_destino_id    UUID NOT NULL REFERENCES conta_bancaria(id),
    valor               NUMERIC(12,2) NOT NULL,
    data                DATE NOT NULL,
    descricao           VARCHAR(255),
    criado_em           TIMESTAMP NOT NULL DEFAULT now(),
    versao              BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_transferencia_bancaria_data ON transferencia_bancaria(data);

-- Seed das 2 contas reais confirmadas na planilha "DRE Financeira Saw" (Caixa do mês: saldo por
-- banco) — mesmo critério de V40 (categoria pré-cadastrada garantida em qualquer ambiente, não só
-- DemoDataSeeder), pra CaixaMensalService.registrarPosicao ter o que resolver desde o primeiro
-- deploy. Admin pode cadastrar mais contas via CRUD (ContaBancariaController).
INSERT INTO conta_bancaria (nome) VALUES ('Itaú'), ('Infinity Pay');
