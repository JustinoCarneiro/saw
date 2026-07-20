-- M26 (change request pós-MVP, item pendente do E14, Blueprint em ROADMAP.md) — merge de verdade
-- entre conta_pagar_receber e lancamento_financeiro: "conta a pagar/receber" e "lançamento" eram,
-- na prática, o mesmo fato financeiro em dois estágios (liquidar() já clonava uma conta liquidada
-- num lançamento novo). Categoria também vira obrigatória em toda linha ("todas as vendas e
-- valores precisam ser mapeados no DRE", confirmado pelo Marcos 19/07/2026) — ver "Achado crítico"
-- no Blueprint. Feito como migration única (schema+dado), mesmo precedente de V28/V29
-- (ALTER COLUMN ... USING em cima de dado real já existente).

-- 1) Colunas absorvidas de conta_pagar_receber.
ALTER TABLE lancamento_financeiro ADD COLUMN data_vencimento DATE;
ALTER TABLE lancamento_financeiro ADD COLUMN data_pagamento DATE;
ALTER TABLE lancamento_financeiro ADD COLUMN valor_pago NUMERIC(12,2);

-- StatusLancamento unificado (PREVISTO, PARCIAL, REALIZADO, VENCIDO — colapsa PAGO/RECEBIDO em
-- REALIZADO, a direção já vem de `tipo`) — precisa vir ANTES do passo 3 (migração de dado): as
-- contas PARCIAL/VENCIDO migradas violariam o CHECK antigo (só PREVISTO/REALIZADO) se ele ainda
-- estivesse valendo na hora do INSERT.
ALTER TABLE lancamento_financeiro DROP CONSTRAINT chk_lancamento_status;
ALTER TABLE lancamento_financeiro ADD CONSTRAINT chk_lancamento_status
    CHECK (status IN ('PREVISTO','PARCIAL','REALIZADO','VENCIDO'));

-- 2) Categorias de receita novas — uma por ProdutoVenda vendável (mapeamento no Blueprint M26).
-- Só MENTORIA_CONTINUA reaproveita OrigemReceita.ASSINATURA (renomeando a categoria "Assinaturas"
-- órfã desde a pausa da Loja/E8 — uq_categoria_financeira_origem_receita, V22, impede duas
-- categorias com a mesma origem não-nula, então as demais ficam com origem_receita NULL: contam
-- no DRE via grupo_dre igual a qualquer categoria de despesa hoje, só não entram no recorte de
-- MRR/composição de receita do dashboard de faturamento — mesma limitação de sempre, não nova).
UPDATE categoria_financeira SET nome = 'Mentoria Contínua'
    WHERE nome = 'Assinaturas' AND origem_receita = 'ASSINATURA';

INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, criado_em, versao)
SELECT gen_random_uuid(), 'Mentoria Contínua', 'RECEITA', 'RECEITA_BRUTA', 'ASSINATURA', now(), 0
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira WHERE nome = 'Mentoria Contínua');

-- "Eventos" (INGRESSO_EVENTO) — antes só existia via DemoDataSeeder (perfil dev/demo); uma
-- instalação de produção sem SEED_DEMO_DATA nunca teria essa categoria pré-criada
-- (CategoriaFinanceiraService reabriu o CRUD manual no Fase 5, mas isso não ajuda
-- LeadService.fecharVenda() a resolver a categoria sozinho). Garantida aqui pelo mesmo motivo das
-- categorias novas acima.
INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, criado_em, versao)
SELECT gen_random_uuid(), 'Eventos', 'RECEITA', 'RECEITA_BRUTA', 'EVENTO', now(), 0
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira WHERE origem_receita = 'EVENTO');

INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, criado_em, versao)
SELECT v.id, v.nome, 'RECEITA', 'RECEITA_BRUTA', NULL, now(), 0 FROM (VALUES
    (gen_random_uuid(), 'Mentoria Individual'),
    (gen_random_uuid(), 'Consultoria'),
    (gen_random_uuid(), 'Fórmula SAW'),
    (gen_random_uuid(), 'Formação Profissional'),
    (gen_random_uuid(), 'Ficha Técnica Lucrativa'),
    (gen_random_uuid(), 'Produtos Digitais')
) AS v(id, nome)
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira c WHERE c.nome = v.nome);

-- Fallback pra dado histórico sem categoria (ex.: parcela de venda criada antes do M26, quando
-- categoria era opcional) — categoria_id em lancamento_financeiro é NOT NULL desde sempre, não dá
-- pra migrar essas linhas sem alguma categoria. Caminho novo (LeadService, pós-M26) sempre resolve
-- uma categoria real — este fallback é só pro dado que já existia antes da migration rodar.
INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, criado_em, versao)
SELECT gen_random_uuid(), 'Vendas (a categorizar)', 'RECEITA', 'RECEITA_BRUTA', NULL, now(), 0
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira WHERE nome = 'Vendas (a categorizar)');

INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, criado_em, versao)
SELECT gen_random_uuid(), 'Despesas (a categorizar)', 'DESPESA', 'DESPESA_OPERACIONAL', NULL, now(), 0
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira WHERE nome = 'Despesas (a categorizar)');

-- 3) Dado: toda conta com um lançamento-gêmeo já materializado (liquidada com criarLancamento=true)
-- tem vencimento/pagamento/valor_pago copiados pra dentro do gêmeo (merge de duas linhas em uma).
UPDATE lancamento_financeiro lf
SET data_vencimento = cpr.data_vencimento, data_pagamento = cpr.data_pagamento, valor_pago = cpr.valor_pago
FROM conta_pagar_receber cpr
WHERE cpr.lancamento_id = lf.id;

-- Toda conta SEM gêmeo (nunca virou Lançamento — PENDENTE/PARCIAL/VENCIDO, ou liquidada com
-- criarLancamento=false) vira uma linha NOVA, preservando o próprio id (permite remapear
-- parcela_venda direto pelo id antigo, sem join extra — ver passo 4).
INSERT INTO lancamento_financeiro (
    id, criado_em, versao, tipo, categoria_id, descricao, valor, data_competencia, status,
    plano_referencia, evento_id, data_vencimento, data_pagamento, valor_pago
)
SELECT
    cpr.id, cpr.criado_em, cpr.versao,
    CASE cpr.tipo WHEN 'A_PAGAR' THEN 'DESPESA' ELSE 'RECEITA' END,
    COALESCE(cpr.categoria_id, (SELECT id FROM categoria_financeira WHERE nome =
        CASE cpr.tipo WHEN 'A_PAGAR' THEN 'Despesas (a categorizar)' ELSE 'Vendas (a categorizar)' END)),
    cpr.descricao, cpr.valor, COALESCE(cpr.data_pagamento, cpr.data_vencimento),
    CASE cpr.status
        WHEN 'PENDENTE' THEN 'PREVISTO'
        WHEN 'PARCIAL' THEN 'PARCIAL'
        WHEN 'PAGO' THEN 'REALIZADO'
        WHEN 'RECEBIDO' THEN 'REALIZADO'
        WHEN 'VENCIDO' THEN 'VENCIDO'
    END,
    NULL, cpr.evento_id, cpr.data_vencimento, cpr.data_pagamento, cpr.valor_pago
FROM conta_pagar_receber cpr
WHERE cpr.lancamento_id IS NULL;

-- 4) parcela_venda passa a apontar direto pro lancamento_financeiro (gêmeo remapeado, ou o próprio
-- id preservado no INSERT acima pras contas sem gêmeo).
ALTER TABLE parcela_venda ADD COLUMN lancamento_financeiro_id UUID;
UPDATE parcela_venda pv
SET lancamento_financeiro_id = COALESCE(cpr.lancamento_id, cpr.id)
FROM conta_pagar_receber cpr
WHERE pv.conta_pagar_receber_id = cpr.id;
ALTER TABLE parcela_venda ADD CONSTRAINT fk_parcela_venda_lancamento
    FOREIGN KEY (lancamento_financeiro_id) REFERENCES lancamento_financeiro(id);
ALTER TABLE parcela_venda DROP COLUMN conta_pagar_receber_id;

-- 5) Retira conta_pagar_receber por completo.
DROP TABLE conta_pagar_receber;

-- 6) Índice novo pro filtro de GET /admin/financeiro/contas (por dataVencimento).
CREATE INDEX idx_lancamento_vencimento ON lancamento_financeiro(data_vencimento);
