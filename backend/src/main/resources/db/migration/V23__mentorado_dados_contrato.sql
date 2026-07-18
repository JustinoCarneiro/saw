-- Change request pós-MVP (reunião 17/07/2026, docs/reuniao-2026-07-17-atualizacoes.md).
-- TipoContrato nasce aditivo, não substitui Plano (ver Suposição 1 do Blueprint M23 no
-- ROADMAP.md) — por isso tipo_contrato é NULLABLE aqui: dado legado/seed não tem essa
-- informação real e não deveria ganhar um valor fabricado.
ALTER TABLE mentorado ADD COLUMN nome_fantasia VARCHAR(255);
ALTER TABLE mentorado ADD COLUMN cnpj BYTEA;
ALTER TABLE mentorado ADD COLUMN socios BYTEA;
ALTER TABLE mentorado ADD COLUMN valor_contrato BYTEA;
ALTER TABLE mentorado ADD COLUMN data_fechamento_contrato DATE;
ALTER TABLE mentorado ADD COLUMN documento_contrato_url VARCHAR(500);
ALTER TABLE mentorado ADD COLUMN tipo_contrato VARCHAR(30);
ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_tipo_contrato
    CHECK (tipo_contrato IS NULL OR tipo_contrato IN ('MENTORIA_CONTINUA','MENTORIA_INDIVIDUAL','CONSULTORIA'));

-- 1:1 com mentorado, mesmo padrão de ata/mentoria (M06): nasce só quando alguém preenche o
-- Diagnóstico Inicial (Léa, antes da 1ª reunião com o Mateus) — não em toda linha de mentorado.
CREATE TABLE mentorado_diagnostico_inicial (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentorado_id              UUID NOT NULL UNIQUE REFERENCES mentorado(id),
    faturamento_anual         BYTEA,
    quantidade_colaboradores  INT,
    empresa_regularizada      BOOLEAN,
    quantidade_lojas          INT,
    cmv_definido              VARCHAR(10),
    cmv_detalhe               VARCHAR(255),
    tempo_medio_atendimento   VARCHAR(100),
    cultura_construida        VARCHAR(20) NOT NULL DEFAULT 'NAO',
    processos_desenhados      VARCHAR(20) NOT NULL DEFAULT 'NAO',
    criado_em                 TIMESTAMP NOT NULL DEFAULT now(),
    versao                    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_diag_cultura CHECK (cultura_construida IN ('SIM','NAO','EM_CONSTRUCAO')),
    CONSTRAINT chk_diag_processos CHECK (processos_desenhados IN ('SIM','NAO','EM_CONSTRUCAO')),
    CONSTRAINT chk_diag_cmv CHECK (cmv_definido IS NULL OR cmv_definido IN ('SIM','NAO'))
);

-- Lead precisa registrar o tipo de contrato fechado, em paralelo a plano_fechado (não substitui —
-- mesma cisão aditiva do Mentorado acima). Usado por "criar mentorado direto" (M23) e pelo import
-- em massa (M24).
ALTER TABLE lead ADD COLUMN tipo_contrato_fechado VARCHAR(30);
ALTER TABLE lead ADD CONSTRAINT chk_lead_tipo_contrato
    CHECK (tipo_contrato_fechado IS NULL OR tipo_contrato_fechado IN ('MENTORIA_CONTINUA','MENTORIA_INDIVIDUAL','CONSULTORIA'));
