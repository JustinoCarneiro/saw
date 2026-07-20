-- E17/M27 (change request pós-MVP, 19/07/2026, item 2) — ranking com as 4 ferramentas
-- obrigatórias nomeadas (DRE estruturada, manual de cultura, ficha técnica, manual de
-- processos). ferramentas_concluidas/ferramentas_total (já existentes) não são tocados aqui —
-- continuam sendo lidos exatamente como antes, só passam a ser recalculados a partir destes 4
-- campos (ver Mentorado#atualizarFerramentasObrigatorias) em vez de editáveis livremente.
ALTER TABLE mentorado ADD COLUMN ferramenta_dre VARCHAR(20) NOT NULL DEFAULT 'NAO';
ALTER TABLE mentorado ADD COLUMN ferramenta_manual_cultura VARCHAR(20) NOT NULL DEFAULT 'NAO';
ALTER TABLE mentorado ADD COLUMN ferramenta_ficha_tecnica VARCHAR(20) NOT NULL DEFAULT 'NAO';
ALTER TABLE mentorado ADD COLUMN ferramenta_manual_processos VARCHAR(20) NOT NULL DEFAULT 'NAO';

ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_ferramenta_dre
    CHECK (ferramenta_dre IN ('SIM', 'NAO', 'EM_CONSTRUCAO'));
ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_ferramenta_manual_cultura
    CHECK (ferramenta_manual_cultura IN ('SIM', 'NAO', 'EM_CONSTRUCAO'));
ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_ferramenta_ficha_tecnica
    CHECK (ferramenta_ficha_tecnica IN ('SIM', 'NAO', 'EM_CONSTRUCAO'));
ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_ferramenta_manual_processos
    CHECK (ferramenta_manual_processos IN ('SIM', 'NAO', 'EM_CONSTRUCAO'));
