-- Gap 1 (raio-x, 18/07/2026, docs/reuniao-2026-07-17-atualizacoes.md § "Implicações pro
-- desenho", item 1): a planilha real do cliente ("Status do Pagamento") usa 3 estados
-- (FALTA PAGAR/PAGO PARCIAL/PAGO) — StatusConta só tinha 2 (mais VENCIDO). valor_pago acumula o
-- que já foi pago enquanto a conta está PARCIAL (ver ContaPagarReceber#liquidarParcial).
ALTER TABLE conta_pagar_receber ADD COLUMN valor_pago NUMERIC(12,2);
ALTER TABLE conta_pagar_receber DROP CONSTRAINT chk_conta_status;
ALTER TABLE conta_pagar_receber ADD CONSTRAINT chk_conta_status
    CHECK (status IN ('PENDENTE','PARCIAL','PAGO','RECEBIDO','VENCIDO'));
