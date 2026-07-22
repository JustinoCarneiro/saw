-- Pedido do Marcos (22/07/2026, ao revisar a tela de DRE) — popular o plano de contas real de
-- despesa (48 subcategorias confirmadas via raio-x da planilha "DRE Financeira Saw", ver
-- docs/reuniao-2026-07-17-atualizacoes.md § "Planilhas reais do Comercial/Financeiro") e usar o
-- campo `grupo` (a "Categoria" real da planilha: Estrutura/Eventos/Financeiro-Jurídico/Marketing/
-- Operação/Outros/Pessoas — os 7 valores confirmados) pra agrupar o gráfico "Despesa por
-- categoria" do DRE no mesmo nível que a planilha mostra no resumo mensal.
--
-- Duas coisas NÃO estão confirmadas linha a linha pelo cliente, então não foram inventadas:
-- 1) A relação subcategoria->categoria abaixo é classificação por semântica do nome (ex.: "Aluguel"
--    -> Estrutura, "V.A e V.T" -> Pessoas), não um dado extraído da planilha real (o raio-x
--    capturou os valores DISTINTOS de cada coluna, não o join linha a linha entre elas). Editável
--    livremente depois — é só o campo `grupo` (texto livre), sem-mudança de schema.
-- 2) `natureza` (Fixa/Variável) fica NULL pras 48 — mesmo critério já usado antes (V41): é
--    confirmado que existe 1 valor fixo por subcategoria na planilha real, mas o raio-x não
--    capturou QUAL é esse valor pra cada uma.
-- `grupo_dre` fica DESPESA_OPERACIONAL pras 48 (mesmo default de "Despesas (a categorizar)", V40)
-- — Custos x Despesa Operacional é classificação de DRE de verdade (afeta o resultado calculado),
-- diferente de `grupo` (só rótulo de agrupamento visual), por isso segue sem inventar CUSTOS pra
-- nenhuma.

INSERT INTO categoria_financeira (id, nome, tipo, grupo_dre, origem_receita, grupo, natureza, criado_em, versao)
SELECT gen_random_uuid(), v.nome, 'DESPESA', 'DESPESA_OPERACIONAL', NULL, v.grupo, NULL, now(), 0
FROM (VALUES
    ('Alimentação Administrativa', 'Operação'),
    ('Alimentação Evento', 'Eventos'),
    ('Almoço Administrativo', 'Operação'),
    ('Almoço/Jantar de Negócios', 'Operação'),
    ('Aluguel', 'Estrutura'),
    ('Apresentador Evento', 'Eventos'),
    ('Brindes Evento', 'Eventos'),
    ('Cartão de Crédito', 'Financeiro/Jurídico'),
    ('Combustível', 'Operação'),
    ('Comercial', 'Operação'),
    ('Condomínio', 'Estrutura'),
    ('Contabilidade', 'Financeiro/Jurídico'),
    ('Coordenador de Projetos', 'Pessoas'),
    ('Custos Viagem Evento', 'Eventos'),
    ('Design', 'Marketing'),
    ('Diretor', 'Pessoas'),
    ('Doação', 'Outros'),
    ('Endomarketing', 'Marketing'),
    ('Energia', 'Estrutura'),
    ('Equipamentos', 'Estrutura'),
    ('Estacionamento', 'Operação'),
    ('Estadia', 'Operação'),
    ('Estorno', 'Financeiro/Jurídico'),
    ('Estrutura Evento', 'Eventos'),
    ('Financeiro (Mentor)', 'Pessoas'),
    ('Hotel', 'Operação'),
    ('Impostos', 'Financeiro/Jurídico'),
    ('Internet', 'Estrutura'),
    ('Jurídico', 'Financeiro/Jurídico'),
    ('Limpeza', 'Estrutura'),
    ('Materiais Evento', 'Eventos'),
    ('Mentoria', 'Pessoas'),
    ('Midia Evento', 'Eventos'),
    ('Mobiliário', 'Estrutura'),
    ('Músico Evento', 'Eventos'),
    ('Outros', 'Outros'),
    ('Palestras Evento', 'Eventos'),
    ('Passagens', 'Operação'),
    ('RH (Mentor)', 'Pessoas'),
    ('Sistemas', 'Estrutura'),
    ('Social Media', 'Marketing'),
    ('Sucesso do Gestor', 'Pessoas'),
    ('Tráfego Pago', 'Marketing'),
    ('Uber', 'Operação'),
    ('V.A e V.T', 'Pessoas'),
    ('Visita Pré-evento', 'Eventos'),
    ('Visitas mentorados', 'Operação'),
    ('Água Mineral', 'Estrutura')
) AS v(nome, grupo)
WHERE NOT EXISTS (SELECT 1 FROM categoria_financeira c WHERE c.nome = v.nome);

-- "Taxas de Plataforma de Pagamento" nasceu na V50 (migration, não seeder) — já existe na hora
-- em que esta migration roda, mesmo num banco 100% novo (migrations rodam em sequência antes de
-- qualquer seed de aplicação), então este UPDATE é efetivo em qualquer ambiente.
UPDATE categoria_financeira SET grupo = 'Financeiro/Jurídico' WHERE nome = 'Taxas de Plataforma de Pagamento' AND grupo IS NULL;

-- "Infraestrutura"/"Marketing"/"Equipe & Folha"/"Impostos sobre vendas" nasceram via
-- DemoDataSeeder (Java, roda DEPOIS de toda migration) — num banco novo elas ainda não existem
-- neste ponto, então o UPDATE abaixo é no-op lá (DemoDataSeeder foi atualizado pra já nascer com
-- `grupo` certo, ver seedFinanceiro()). Mantido só como rede de segurança pra um banco de dev que
-- já tinha rodado o seed ANTES desta migration existir (ex.: o ambiente em que isto foi escrito).
UPDATE categoria_financeira SET grupo = 'Estrutura' WHERE nome = 'Infraestrutura' AND grupo IS NULL;
UPDATE categoria_financeira SET grupo = 'Marketing' WHERE nome = 'Marketing' AND grupo IS NULL;
UPDATE categoria_financeira SET grupo = 'Pessoas' WHERE nome = 'Equipe & Folha' AND grupo IS NULL;
UPDATE categoria_financeira SET grupo = 'Financeiro/Jurídico' WHERE nome = 'Impostos sobre vendas' AND grupo IS NULL;
