-- M09: E3 · Metas Estratégicas (ver ROADMAP.md)

-- Máquina de estado (CLAUDE.md): Ativa {No prazo | Atenção | Atrasada} -> Concluída | desvio -> Pausada.
-- Só os 3 estados topo são persistidos; o sub-status (No prazo/Atenção/Atrasada) é calculado no
-- response a partir de prazo vs hoje, nunca gravado.
CREATE TABLE meta (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id),
    titulo          VARCHAR(255) NOT NULL,
    descricao       VARCHAR(1000),
    prazo           DATE NOT NULL,
    progresso_pct   INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_meta_status CHECK (status IN ('ATIVA','CONCLUIDA','PAUSADA')),
    CONSTRAINT chk_meta_progresso CHECK (progresso_pct BETWEEN 0 AND 100)
);

CREATE INDEX idx_meta_mentorado ON meta(mentorado_id);
CREATE INDEX idx_meta_status ON meta(status);
