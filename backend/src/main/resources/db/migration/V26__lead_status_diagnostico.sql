-- M25 — achado ao vivo via E2E: chk_lead_status (V4) nunca foi atualizado quando
-- StatusLead.DIAGNOSTICO entrou (V25 só mexeu nas colunas de venda, não nesta constraint,
-- porque DIAGNOSTICO é valor de enum Java, sem coluna nova associada). Sem isto, mover um Lead
-- pra Diagnóstico estoura "violates check constraint chk_lead_status" em produção.
ALTER TABLE lead DROP CONSTRAINT chk_lead_status;
ALTER TABLE lead ADD CONSTRAINT chk_lead_status
    CHECK (status IN ('SOLICITACAO','EM_CONTATO','DIAGNOSTICO','PROPOSTA','FECHADO','PERDIDO'));
