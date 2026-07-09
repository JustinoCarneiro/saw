-- M10: E4 · Tarefas & Agenda (ver ROADMAP.md)

-- Máquina de estado (CLAUDE.md): Pendente -> Em andamento -> Concluída | Atrasada quando prazo
-- vence sem conclusão (calculada no response, não persistida — mesmo padrão do sub-status de Meta).
ALTER TABLE encaminhamento ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';
UPDATE encaminhamento SET status = 'CONCLUIDA' WHERE concluido = true;
ALTER TABLE encaminhamento ADD CONSTRAINT chk_encaminhamento_status
    CHECK (status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDA'));
ALTER TABLE encaminhamento DROP COLUMN concluido;

-- Nullable de propósito: encaminhamentos antigos e os gerados por ata não têm prazo, só as
-- tarefas self-service novas exigem (ver ROADMAP.md M10).
ALTER TABLE encaminhamento ADD COLUMN prazo DATE;
ALTER TABLE encaminhamento ADD COLUMN prioridade VARCHAR(10) NOT NULL DEFAULT 'MEDIA';
ALTER TABLE encaminhamento ADD CONSTRAINT chk_encaminhamento_prioridade
    CHECK (prioridade IN ('ALTA','MEDIA','BAIXA'));

-- Vínculo a metas (CLAUDE.md § E4) — nullable, nem toda tarefa está ligada a uma meta.
ALTER TABLE encaminhamento ADD COLUMN meta_id UUID REFERENCES meta(id);

CREATE INDEX idx_encaminhamento_status ON encaminhamento(status);
