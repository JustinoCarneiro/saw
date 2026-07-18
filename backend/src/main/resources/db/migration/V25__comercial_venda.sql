-- Change request pós-MVP (reunião 17/07/2026, docs/reuniao-2026-07-17-atualizacoes.md § E13).
-- Aditivo em Lead — convive com plano_fechado/tipo_contrato_fechado (M23), não substitui (ver
-- Suposições do Blueprint M25 no ROADMAP.md).
ALTER TABLE lead ADD COLUMN produto_venda VARCHAR(30);
ALTER TABLE lead ADD COLUMN origem_venda VARCHAR(20);
ALTER TABLE lead ADD COLUMN valor_total_venda BYTEA;
ALTER TABLE lead ADD COLUMN valor_pago_no_ato BYTEA;
ALTER TABLE lead ADD COLUMN forma_pagamento VARCHAR(20);
ALTER TABLE lead ADD CONSTRAINT chk_lead_produto_venda
    CHECK (produto_venda IS NULL OR produto_venda IN
        ('MENTORIA_CONTINUA','MENTORIA_INDIVIDUAL','CONSULTORIA','INGRESSO_EVENTO','PRODUTO_DIGITAL'));
ALTER TABLE lead ADD CONSTRAINT chk_lead_origem_venda
    CHECK (origem_venda IS NULL OR origem_venda IN ('DIRETA','HOTMART','CORTESIA','PATROCINIO','PALESTRANTE'));
ALTER TABLE lead ADD CONSTRAINT chk_lead_forma_pagamento
    CHECK (forma_pagamento IS NULL OR forma_pagamento IN ('PIX','CARTAO','BOLETO','HOTMART'));

-- Parcelamento estruturado — não existia em lugar nenhum antes (nem coluna solta, nem tabela).
-- Cada parcela gera automaticamente um conta_pagar_receber A_RECEBER (categoria opcional de
-- propósito — plano de contas hierárquico é trabalho do E14, fora desta leva).
CREATE TABLE parcela_venda (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id                 UUID NOT NULL REFERENCES lead(id),
    numero                  INT NOT NULL,
    valor                   BYTEA NOT NULL,
    data_prevista           DATE NOT NULL,
    conta_pagar_receber_id  UUID REFERENCES conta_pagar_receber(id),
    criado_em               TIMESTAMP NOT NULL DEFAULT now(),
    versao                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_parcela_venda_numero UNIQUE (lead_id, numero)
);

-- Uma linha por ingresso (não por venda) — credenciamento é nominal, uma venda de 2 ingressos
-- gera 2 registros (mesmo padrão das planilhas reais "Vendas Eventos"/"CREDENCIAMENTO").
CREATE TABLE venda_ingresso (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id             UUID NOT NULL REFERENCES lead(id),
    evento_id           UUID NOT NULL REFERENCES evento(id),
    categoria_ingresso  VARCHAR(20) NOT NULL,
    nome_credenciado    BYTEA,
    setor               VARCHAR(100),
    almoco              BOOLEAN NOT NULL DEFAULT false,
    check_in            BOOLEAN NOT NULL DEFAULT false,
    criado_em           TIMESTAMP NOT NULL DEFAULT now(),
    versao              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_venda_ingresso_categoria CHECK (categoria_ingresso IN ('CORTESIA','ESSENCIAL','VIP','ESPECIAL'))
);
