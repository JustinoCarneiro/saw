-- Gap 7 (raio-x em "Vendas Aline Melo" + pesquisa da taxa real da Hotmart, confirmado 19/07/2026):
-- gateways de pagamento retêm uma fatia da venda antes de repassar pra SAW (Hotmart: ~9,9%+R$1,
-- mais taxa de antecipação opcional). Sem esse campo, valor_pago_no_ato < valor_total_venda
-- parecia dívida do cliente quando na verdade ele pagou 100% — a diferença é taxa de plataforma
-- retida, não parcela em aberto. Mesmo critério pgcrypto de valor_total_venda/valor_pago_no_ato
-- (V25) — dado financeiro específico de uma venda identificável.
ALTER TABLE lead ADD COLUMN taxa_plataforma_retida BYTEA;
