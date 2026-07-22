-- Change request pós-MVP (comercial/financeiro, reunião 17/07/2026):
--
-- 1) "Patrocínio" — bucket de receita confirmado no raio-x da planilha real "DRE Financeira Saw"
-- (Receita (resumo): Total, Fixas, Variáveis, Eventos, Mentoria Contínua, Mentoria Individual,
-- Patrocínio, Produtos Digitais) — único desses ainda sem CategoriaFinanceira correspondente
-- (os demais já vieram de V40). Mesmo padrão idempotente de V40 (WHERE NOT EXISTS).
INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, criado_em, versao)
SELECT gen_random_uuid(), 'Patrocínio', 'RECEITA', 'RECEITA_BRUTA', NULL, now(), 0
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira WHERE nome = 'Patrocínio');

-- 2) "Taxas de Plataforma de Pagamento" — gap 7 (taxaPlataformaRetida, confirmado 19/07/2026):
-- até aqui só entrava na tela de Conciliação, nunca no DRE. Vira DEDUCOES (mesmo grupo de
-- "Impostos sobre vendas" no DemoDataSeeder) — Receita Bruta já passa a incluir o valor bruto da
-- venda (pago no ato + taxa retida), e esta categoria deduz a taxa de volta pra chegar na Receita
-- Líquida de verdade recebida pela SAW (ver LeadService.criarLancamentoValorPagoNoAto).
INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, criado_em, versao)
SELECT gen_random_uuid(), 'Taxas de Plataforma de Pagamento', 'DESPESA', 'DEDUCOES', NULL, now(), 0
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira WHERE nome = 'Taxas de Plataforma de Pagamento');
