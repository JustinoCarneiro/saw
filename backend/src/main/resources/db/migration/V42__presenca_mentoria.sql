-- E17/M27 (change request pós-MVP, 19/07/2026, item 1 de "Perguntas e pendências pro Victor" —
-- decisão de produto tomada sem resposta do cliente ainda, ver ROADMAP.md § "Blueprint (M27)").
-- Presença por mentorado numa mentoria GRUPO (individual já é coberta pelo status da sessão
-- inteira). Aditivo: não mexe em mentoria_mentorado, que já existe e continua igual.
CREATE TABLE presenca_mentoria (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentoria_id  UUID NOT NULL REFERENCES mentoria(id),
    mentorado_id UUID NOT NULL REFERENCES mentorado(id),
    presente     BOOLEAN NOT NULL,
    criado_em    TIMESTAMP NOT NULL DEFAULT now(),
    versao       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_presenca_mentoria UNIQUE (mentoria_id, mentorado_id)
);
