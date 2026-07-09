-- M13: E7 · Eventos & Inscrições (lado mentorado) — H7.2.
ALTER TABLE evento ADD COLUMN vagas_ocupadas INT NOT NULL DEFAULT 0;

CREATE TABLE inscricao_evento (
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id) ON DELETE CASCADE,
    evento_id       UUID NOT NULL REFERENCES evento(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'INSCRITA',
    versao          BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (mentorado_id, evento_id),
    CONSTRAINT chk_inscricao_status CHECK (status IN ('INSCRITA','CANCELADA','PARTICIPOU'))
);

-- evento nunca ganhou esses dois índices no V5 (M06) — este módulo é o primeiro a filtrar/
-- ordenar por eles em volume (agenda + calendário do mentorado).
CREATE INDEX idx_evento_status ON evento(status);
CREATE INDEX idx_evento_data_hora ON evento(data_hora);
CREATE INDEX idx_inscricao_evento_mentorado ON inscricao_evento(mentorado_id);
