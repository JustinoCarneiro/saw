# ROADMAP — SAW HUB

> Blueprint gerado na Fase 3 da metodologia Onda-Dev.
> Fonte da verdade técnica: consumir junto com o `CLAUDE.md`.

## Mapa de fases e skills

| Fase | Skill | Entregável |
|---|---|---|
| 1 · Spec Viva | `onda-spec-viva` | `CLAUDE.md` + `docs/spec.md` |
| 2 · Layout | `onda-direcao-visual` (se sem identidade) + `onda-layout` | Protótipo aprovado |
| 3 · Blueprint | `onda-blueprint` | Este ROADMAP + contratos |
| 4 · XP Coding | `onda-xp-tdd` | Módulos testados e commitados |
| 5 · Homologação | `onda-homologacao` | Deploy em produção |

## Nota sobre esta Fase 3

A Fase 3 foi pulada pra todo o sistema no início do MVP (decisão explícita, por urgência do cliente
— ver `CLAUDE.md`). **E1 (Autenticação), E15 (Gestão de Time) e E17 (Painel Consolidado) já foram
implementados sem Blueprint formal** nessa leva inicial; a lacuna foi coberta depois com testes
unitários retroativos e revisão de segurança (`revisor-seguranca`), não com este documento.

A partir de **E14 · Financeiro & DRE**, retomamos a Fase 3 normalmente: Blueprint antes do código,
por decisão do cliente/Marcos. Este ROADMAP cobre o módulo E14; os módulos seguintes (E13 Comercial,
E11 Mentoria+Ata+IA, módulos do mentorado) ganham sua própria seção aqui quando entrarem na esteira.

## Módulos

| ID | Módulo | Peso | Dias | Histórias |
|---|---|---|---|---|
| M01 | E1 · Autenticação & Acesso | Grande · risco alto | — (já implementado, sem Blueprint formal) | H1.1–H1.x |
| M02 | E15 · Gestão de Time (RBAC por área) | Médio · risco alto | — (já implementado, sem Blueprint formal) | H15.1–H15.5 |
| M03 | E17 · Painel Consolidado & Ranking | Grande · risco médio | — (já implementado, sem Blueprint formal) | H17.1–H17.4 |
| **M04** | **E14 · Financeiro & DRE** | **Grande · risco alto** | **6d** | **H14.1–H14.4** |
| **M05** | **E13 · Comercial & Vendas** | **Grande** | **6d + ~1d (fast-follow H1.3)** | **H13.1–H13.3 + H1.3** |

### M04 — E14 · Financeiro & DRE

**Por que Grande · risco alto:**
- **Complexidade:** DRE com hierarquia de agregação (receita bruta → deduções → receita líquida →
  custos → despesas operacionais → resultado), comparativo mês a mês, dashboard de faturamento com
  MRR e churn (exige olhar período atual vs anterior, não só somar linhas), 3 entidades novas cada
  uma com sua própria máquina de estado.
- **Risco:** é dado financeiro real do negócio da SAW — erro de cálculo aqui vira decisão errada pro
  Fundador (o próprio usuário do módulo). Segundo módulo do sistema (depois de Auth) a exigir
  `revisor-seguranca` obrigatório antes de fechar, por definição do `CLAUDE.md`.

**Correção pós-Blueprint (pgcrypto):** esta seção originalmente previa criptografia a nível de
coluna (`pgcrypto`) pro `valor` de `lancamento_financeiro`/`conta_pagar_receber`. Revisando contra
`docs/spec.md` (H14.1–H14.4 não citam criptografia) e o `CLAUDE.md` (a cláusula de `pgcrypto` fala
em "financeiro **do mentorado**, dados pessoais" — este módulo é o DRE **interno da SAW**, não dado
de mentorado), o Blueprint superestimou o escopo aqui. Decisão (Marcos, ver histórico de decisão):
**deferir `pgcrypto` para um pass transversal na Fase 5 · Homologação**, cobrindo todas as colunas
sensíveis do sistema de uma vez (inclui dados de mentorado quando existirem) em vez de reescrever a
camada de agregação do DRE agora e de novo quando as próximas tabelas entrarem no mesmo esforço —
`SUM()`/`GROUP BY` nativos do Postgres não operam sobre `bytea` criptografado, então criptografia de
coluna aqui implicaria decriptar linha a linha em memória em todo `RelatorioFinanceiroService`.
TLS em trânsito e criptografia de disco da VPS (`CLAUDE.md` § Segurança e persistência dos dados) já
cobrem uma camada de proteção em repouso nesse meio-tempo.

## Modelagem de banco (M04 · E14)

```sql
CREATE TABLE categoria_financeira (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(120) NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,   -- RECEITA | DESPESA
    grupo_dre       VARCHAR(30)  NOT NULL,   -- RECEITA_BRUTA | DEDUCOES | CUSTOS | DESPESA_OPERACIONAL
    origem_receita  VARCHAR(20),             -- ASSINATURA | LOJA | EVENTO | OUTRA (só quando tipo=RECEITA)
    criado_em       TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_categoria_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT chk_categoria_grupo_dre CHECK (grupo_dre IN ('RECEITA_BRUTA','DEDUCOES','CUSTOS','DESPESA_OPERACIONAL')),
    CONSTRAINT chk_categoria_origem CHECK (origem_receita IS NULL OR origem_receita IN ('ASSINATURA','LOJA','EVENTO','OUTRA'))
);

-- Máquina de estado (CLAUDE.md): Previsto -> Realizado
CREATE TABLE lancamento_financeiro (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo              VARCHAR(20)  NOT NULL,  -- RECEITA | DESPESA (redundante c/ categoria, útil pra índice/filtro direto)
    categoria_id      UUID NOT NULL REFERENCES categoria_financeira(id),
    descricao         VARCHAR(255) NOT NULL,
    valor             NUMERIC(12,2) NOT NULL,
    data_competencia  DATE NOT NULL,          -- mês/ano de referência pro DRE, não a data de digitação
    status            VARCHAR(20)  NOT NULL DEFAULT 'PREVISTO',
    plano_referencia  VARCHAR(30),            -- só quando origem_receita=ASSINATURA, alimenta o MRR por plano
    criado_em         TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_lancamento_tipo CHECK (tipo IN ('RECEITA','DESPESA')),
    CONSTRAINT chk_lancamento_status CHECK (status IN ('PREVISTO','REALIZADO'))
);

-- Máquina de estado (CLAUDE.md): A pagar/A receber -> Pago/Recebido (ou Vencido)
CREATE TABLE conta_pagar_receber (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo             VARCHAR(20)  NOT NULL,  -- A_PAGAR | A_RECEBER
    descricao        VARCHAR(255) NOT NULL,
    valor            NUMERIC(12,2) NOT NULL,
    data_vencimento  DATE NOT NULL,
    data_pagamento   DATE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',  -- PENDENTE | PAGO | RECEBIDO | VENCIDO
    lancamento_id    UUID REFERENCES lancamento_financeiro(id), -- setado quando liquidada (gera o Realizado correspondente)
    criado_em        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_conta_tipo CHECK (tipo IN ('A_PAGAR','A_RECEBER')),
    CONSTRAINT chk_conta_status CHECK (status IN ('PENDENTE','PAGO','RECEBIDO','VENCIDO'))
);

CREATE INDEX idx_lancamento_competencia ON lancamento_financeiro(data_competencia);
CREATE INDEX idx_lancamento_categoria ON lancamento_financeiro(categoria_id);
CREATE INDEX idx_conta_vencimento ON conta_pagar_receber(data_vencimento);
CREATE INDEX idx_conta_status ON conta_pagar_receber(status);
```

**Regra de consistência tipo↔status** (`conta_pagar_receber`): `A_PAGAR` só termina em `PAGO`;
`A_RECEBER` só termina em `RECEBIDO` — validado na camada de serviço (Java), não no banco, pra
manter a mensagem de erro amigável em vez de uma violação de CHECK genérica.

**Job agendado** (Spring `@Scheduled`, já decidido no `CLAUDE.md` como mecanismo de agendamento do
projeto): roda diariamente, marca `PENDENTE → VENCIDO` toda `conta_pagar_receber` cuja
`data_vencimento < hoje` e `status = PENDENTE`.

## Contratos de API (M04 · E14)

Todas as rotas sob `/api/v1/admin/financeiro/**`, `@RequiresModulo(Modulo.FINANCEIRO)` — e é aqui
que entra o item M4 da revisão de segurança: **default-deny explícito no filter chain** por rota
(`.requestMatchers("/api/v1/admin/financeiro/**").hasAuthority("MODULO_FINANCEIRO")`), não só a
anotação opt-in, já que este é o módulo de maior sensibilidade do sistema.

### `POST /api/v1/admin/financeiro/lancamentos`
```jsonc
// Request
{
  "tipo": "RECEITA",              // RECEITA | DESPESA
  "categoriaId": "uuid",
  "descricao": "Assinatura Profissional — João Silva",
  "valor": 397.00,
  "dataCompetencia": "2026-07-01",
  "status": "REALIZADO",          // PREVISTO | REALIZADO
  "planoReferencia": "PROFISSIONAL" // opcional, só quando origem_receita=ASSINATURA
}
// Response 201
{
  "id": "uuid",
  "tipo": "RECEITA",
  "categoria": { "id": "uuid", "nome": "Assinaturas", "origemReceita": "ASSINATURA" },
  "descricao": "Assinatura Profissional — João Silva",
  "valor": 397.00,
  "dataCompetencia": "2026-07-01",
  "status": "REALIZADO",
  "planoReferencia": "PROFISSIONAL"
}
```

### `GET /api/v1/admin/financeiro/lancamentos?de=2026-07-01&ate=2026-07-31&tipo=&categoriaId=`
Resposta: `LancamentoResponse[]` (mesmo shape do POST acima).

### `GET /api/v1/admin/financeiro/dre?ano=2026&mes=7`
```jsonc
// Response 200
{
  "periodo": "2026-07",
  "receitaBruta": 28500.00,
  "deducoes": 1200.00,
  "receitaLiquida": 27300.00,
  "custos": 8400.00,
  "despesasOperacionais": 6200.00,
  "resultado": 12700.00,
  "comparativoMesAnterior": { "resultado": 11100.00, "variacaoPct": 14.4 }
}
```

### `GET /api/v1/admin/financeiro/dashboard-faturamento?ano=2026&mes=7`
```jsonc
// Response 200
{
  "faturamentoMensal": 28500.00,
  "mrr": 21400.00,
  "churnPct": 3.1,
  "composicao": [
    { "origem": "ASSINATURA", "valor": 21400.00 },
    { "origem": "LOJA", "valor": 5100.00 },
    { "origem": "EVENTO", "valor": 2000.00 }
  ]
}
```

### `GET /api/v1/admin/financeiro/contas?tipo=A_PAGAR&status=PENDENTE`
Resposta: `ContaResponse[]` — `{ id, tipo, descricao, valor, dataVencimento, dataPagamento, status }`.

### `POST /api/v1/admin/financeiro/contas`
```jsonc
// Request
{ "tipo": "A_PAGAR", "descricao": "Servidor Hostinger — julho", "valor": 180.00, "dataVencimento": "2026-07-10" }
// Response 201: ContaResponse (status inicial PENDENTE)
```

### `PATCH /api/v1/admin/financeiro/contas/{id}/liquidar`
```jsonc
// Request
{ "dataPagamento": "2026-07-08", "criarLancamento": true }
// Response 200: ContaResponse (status PAGO ou RECEBIDO conforme o tipo), com lancamentoId preenchido se criarLancamento=true
```

## Rastreabilidade história ↔ módulo (M04)

| História | Cobertura |
|---|---|
| H14.1 — lançar receitas/despesas | `POST/GET /lancamentos` |
| H14.2 — DRE por período | `GET /dre` |
| H14.3 — dashboard de faturamento | `GET /dashboard-faturamento` |
| H14.4 — contas a pagar/receber com status | `POST/GET /contas`, `PATCH /contas/{id}/liquidar`, job `@Scheduled` de vencimento |

### M05 — E13 · Comercial & Vendas

**Por que Grande:**
- **Complexidade:** funil de vendas com máquina de estado própria (`Lead comercial` do `CLAUDE.md`:
  `Solicitação → Em contato → Proposta → Fechado` · desvio `→ Perdido`), dashboard cruzando dado
  próprio (leads) com dado de outro módulo (MRR/vendas da loja, já calculados no Financeiro), e
  metas/ranking por vendedor.
- **Risco não listado no `CLAUDE.md` mas real:** este módulo introduz o **primeiro endpoint público
  não-autenticado do sistema além do login** (`POST /api/v1/leads`, ver nota H1.3 abaixo) — validação
  de entrada e ausência de vazamento de dado de pipeline pra quem não está logado merecem
  `revisor-seguranca` mesmo sem a tag formal "risco alto", pela superfície de ataque nova, não por
  reclassificar o módulo inteiro.

**Achado de Blueprint — H1.3 ("Solicitar acesso") nunca foi implementado:** `docs/spec.md` define
leads como "solicitações de acesso" (H13.2), mas H1.3 (E1) — o formulário público que gera essa
solicitação — não existe em `backend/` nem `frontend/` (E1 foi construído sem Blueprint formal, ver
nota no topo deste documento, e essa história ficou pra trás). Em vez de modelar "Lead" como um
conceito paralelo e duplicado, este Blueprint **fecha H1.3 como pré-requisito de H13.2**: uma única
entidade `Lead` nasce da solicitação pública (status inicial `SOLICITACAO`) e progride pelo funil
comercial. A tabela de status do M01 (E1) é corrigida abaixo pra refletir essa pendência.

**Fora de escopo, de propósito:** fechar um lead (`status=FECHADO`) registra a venda pra fins de
métrica (plano fechado, data), mas **não cria a conta de login do mentorado** — isso é
explicitamente `CRUD de mentorados por plano` do **E11 · Gestão Admin** (`CLAUDE.md`), não deste
módulo. Ligar `Lead → Mentorado` de verdade é um fast-follow natural quando E11 existir, não algo a
antecipar aqui — mesmo raciocínio que evitou o over-scope do `pgcrypto` no M04.

**Achados da revisão de segurança (`revisor-seguranca`), todos corrigidos:**
- **M1:** `POST /api/v1/leads` é o único endpoint de escrita sem autenticação do sistema — sem
  limite, um script podia inflar a tabela `lead` indefinidamente. Corrigido com rate limit por IP
  via Redis (`LeadRateLimitFilter`, 5 solicitações/10min). O achado também apontou que
  `LeadService.listar` buscava a tabela inteira e filtrava em memória Java, ignorando os índices
  `idx_lead_status`/`idx_lead_vendedor` — corrigido movendo o filtro pra dentro da query JPQL.
- **M2:** `/api/v1/admin/comercial/**` reexpõe `mrr`/`vendasLoja` (mesma classe de dado do
  Financeiro, protegida por default-deny explícito desde o M4) através de um caminho defendido só
  pelo `@RequiresModulo` opt-in. Corrigido com o mesmo tratamento do M4: matcher explícito
  `hasAuthority("MODULO_COMERCIAL")` no filter chain.
- **L2:** `fechar()`/`perder()` aceitavam `planoFechado`/`motivoPerdido` nulos, quebrando
  silenciosamente "vendas por plano". Corrigido exigindo os dois na camada de serviço.
- **L3 (PII sem `pgcrypto`):** `lead.email`/`telefone`/`nome` são dado pessoal (LGPD) em texto
  puro — mesma lacuna do `pgcrypto` já identificada e deferida no M04. **Adicionado ao mesmo pass
  transversal da Fase 5 · Homologação**, não tratado isoladamente aqui (ver tabela de pipeline).

**Bug achado ao vivo na verificação pós-fixes (curl), corrigido com teste de regressão:**
`LeadService.avancar()` usava `leadRepository.findById()` puro, que devolve `vendedor` como proxy
`LAZY` não inicializado (`Lead.vendedor` é `FetchType.LAZY`). A própria transição pra `EM_CONTATO`
mascarava o problema (o vendedor vem de `colaboradorRepository.findById()`, já totalmente
carregado), mas qualquer transição seguinte sem esse parâmetro (`EM_CONTATO → PROPOSTA`,
`→ FECHADO`, `→ PERDIDO`) devolvia 500 (`LazyInitializationException`) ao montar o `LeadResponse`
fora da transação (`open-in-view=false`) — a gravação em si tinha sucesso, só a resposta HTTP
quebrava. Corrigido com `LeadRepository.buscarPorIdComVendedor` (mesmo padrão `LEFT JOIN FETCH` já
usado em `buscarComFiltro`), com teste de regressão `@DataJpaTest` (`LeadRepositoryTest`, sessão
real do Hibernate — um teste baseado em mock nunca reproduziria isso) e reforçado no E2E
(`comercial.spec.ts`, o pipeline completo passa por essa transição). Ver "Verificar todos os fixes
ao vivo via curl" na tabela de pipeline — é exatamente esse tipo de achado que essa etapa existe
pra pegar antes da Fase 5.

## Modelagem de banco (M05 · E13)

```sql
-- Máquina de estado (CLAUDE.md): Solicitação -> Em contato -> Proposta -> Fechado | desvio: -> Perdido
CREATE TABLE lead (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome              VARCHAR(120) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    telefone          VARCHAR(20),
    mensagem          VARCHAR(500),
    plano_interesse   VARCHAR(20),              -- GRATUITO|BASICO|ESSENCIAL|PROFISSIONAL, opcional (visitante pode não saber)
    status            VARCHAR(20)  NOT NULL DEFAULT 'SOLICITACAO',
    vendedor_id       UUID REFERENCES colaborador(id),   -- setado a partir de EM_CONTATO
    plano_fechado     VARCHAR(20),              -- setado só quando status=FECHADO
    motivo_perdido    VARCHAR(255),             -- setado só quando status=PERDIDO
    data_fechamento   TIMESTAMP,                -- setado em FECHADO ou PERDIDO (filtro "no mês")
    criado_em         TIMESTAMP    NOT NULL DEFAULT now(),
    versao            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_lead_status CHECK (status IN ('SOLICITACAO','EM_CONTATO','PROPOSTA','FECHADO','PERDIDO')),
    CONSTRAINT chk_lead_plano_interesse CHECK (plano_interesse IS NULL OR plano_interesse IN ('GRATUITO','BASICO','ESSENCIAL','PROFISSIONAL')),
    CONSTRAINT chk_lead_plano_fechado CHECK (plano_fechado IS NULL OR plano_fechado IN ('GRATUITO','BASICO','ESSENCIAL','PROFISSIONAL'))
);

CREATE INDEX idx_lead_status ON lead(status);
CREATE INDEX idx_lead_vendedor ON lead(vendedor_id);
CREATE INDEX idx_lead_data_fechamento ON lead(data_fechamento);

-- H13.3: meta por vendedor/período, comparada contra o realizado (COUNT de leads FECHADO no período)
CREATE TABLE meta_comercial (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id       UUID NOT NULL REFERENCES colaborador(id),
    ano               INT  NOT NULL,
    mes               INT  NOT NULL,
    meta_fechamentos  INT  NOT NULL,
    criado_em         TIMESTAMP NOT NULL DEFAULT now(),
    versao            BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_meta_vendedor_periodo UNIQUE (vendedor_id, ano, mes)
);
```

**Reaproveitamento do Financeiro (não duplicar dado):** `mrr` e `vendasLoja` no dashboard (H13.1)
leem direto de `lancamento_financeiro` via `LancamentoFinanceiroRepository` (mesmo filtro por
`OrigemReceita` que `RelatorioFinanceiroService` já usa) — o módulo Comercial não guarda seu próprio
número de faturamento, só agrega sobre o que o Financeiro já é dono, mesmo padrão que
`consolidated/` já usa sobre `mentorado/`. `vendasLoja` fica em R$ 0 até o E8 (Loja) existir — é
consequência esperada da ordem de construção, não um bug (mesmo caso do E17 rodando sobre dado seed
antes do E4 completo).

## Contratos de API (M05 · E13)

### `POST /api/v1/leads` — público, `permitAll()` (fecha H1.3)
```jsonc
// Request
{ "nome": "Maria Souza", "email": "maria@restaurante.com", "telefone": "11999998888",
  "mensagem": "Quero saber mais sobre o plano Essencial", "planoInteresse": "ESSENCIAL" }
// Response 201 — resposta mínima, não expõe dado de pipeline pra quem não está autenticado
{ "id": "uuid", "status": "SOLICITACAO" }
```

### `GET /api/v1/admin/comercial/leads?status=&vendedorId=`
`@RequiresModulo(Modulo.COMERCIAL)`. Resposta: `LeadResponse[]` — funil (H13.2).
```jsonc
[{ "id": "uuid", "nome": "Maria Souza", "email": "...", "telefone": "...",
   "planoInteresse": "ESSENCIAL", "status": "EM_CONTATO",
   "vendedor": { "id": "uuid", "nome": "Paula" }, "criadoEm": "2026-07-08T12:00:00Z" }]
```

### `PATCH /api/v1/admin/comercial/leads/{id}/avancar`
Transições válidas idênticas à máquina de estado do `CLAUDE.md`; inválidas retornam 409 (mesmo
`GlobalExceptionHandler` de `IllegalStateException` já usado pelo M04).
```jsonc
// Avançar / atribuir vendedor
{ "novoStatus": "EM_CONTATO", "vendedorId": "uuid" }
// Fechar
{ "novoStatus": "FECHADO", "planoFechado": "ESSENCIAL" }
// Perder
{ "novoStatus": "PERDIDO", "motivoPerdido": "Optou por concorrente" }
// Response 200: LeadResponse atualizado
```

### `GET /api/v1/admin/comercial/dashboard?ano=2026&mes=7`
```jsonc
{
  "novosMentoradosNoMes": 4,      // leads FECHADO no período
  "taxaConversaoPct": 28.5,       // FECHADO / (FECHADO + PERDIDO) no período
  "mrr": 21400.00,                // lido do Financeiro (OrigemReceita.ASSINATURA)
  "vendasLoja": 5100.00,          // lido do Financeiro (OrigemReceita.LOJA) — 0 até o E8 existir
  "variacaoMrrPct": 8.2,
  "funil": [
    { "status": "SOLICITACAO", "quantidade": 12 },
    { "status": "EM_CONTATO", "quantidade": 5 },
    { "status": "PROPOSTA", "quantidade": 3 },
    { "status": "FECHADO", "quantidade": 4 },
    { "status": "PERDIDO", "quantidade": 2 }
  ]
}
```

### `GET /api/v1/admin/comercial/ranking?ano=2026&mes=7`
```jsonc
[{ "vendedor": { "id": "uuid", "nome": "Paula" }, "metaFechamentos": 6, "realizado": 4, "pctAtingido": 66.7 }]
```

### `GET /api/v1/admin/comercial/vendedores`
Endpoint auxiliar, não previsto no Blueprint original — surgiu na construção do frontend: o
seletor de vendedor da tela de funil (mover lead pra Em contato) precisa listar colaboradores da
área Comercial, e `TeamController` (`/admin/team`) é gated por `Modulo.TIME` (só Fundador), então
uma área Comercial não-Fundador não conseguiria montar esse dropdown por ali. Mesmo padrão do
`CategoriaFinanceiraController` do M04 (só leitura, repositório injetado direto no controller).
```jsonc
[{ "id": "uuid", "nome": "Paula Mendes" }]
```

## Rastreabilidade história ↔ módulo (M05)

| História | Cobertura |
|---|---|
| H1.3 — solicitar acesso (gap de E1 fechado aqui) | `POST /api/v1/leads` (público) |
| H13.1 — dashboard comercial | `GET /comercial/dashboard` |
| H13.2 — funil de vendas | `GET /comercial/leads`, `PATCH /comercial/leads/{id}/avancar` |
| H13.3 — metas e ranking do time | `GET /comercial/ranking` (+ seed de `meta_comercial`, sem CRUD dedicado nesta leva) |

## Fórmula de prazo

```
Prazo = Fase 2 (2d ou 4d se sem identidade) + Σ(dias dos módulos) + 2d (Fase 5)
```

Fase 2 já entregue (protótipo aprovado e congelado). M04 (E14, concluído) somou **6 dias** de
engenharia; M05 (E13) soma mais **6d + ~1d** (o dia extra cobre o fast-follow de H1.3, achado
faltando durante este Blueprint) — os módulos seguintes (E11+IA, mentorado) somam seus próprios
dias quando ganharem Blueprint, na ordem em que entrarem.

## Pipeline geral até a conclusão do MVP

Cada linha abaixo passa pelo mesmo ciclo: **Blueprint (Fase 3, leve) → Red-Green-Refactor (Fase 4)
→ `revisor-seguranca` nos módulos de risco → E2E Playwright → commit limpo.** Dias = peso da
tabela da metodologia (Pequeno 1–2d / Médio 3–4d / Grande 5–7d), não hora-homem literal — é a
métrica de comparação entre módulos, não uma promessa de calendário.

| # | Módulo | Peso | Dias | Status |
|---|---|---|---|---|
| — | E1 · Autenticação & Acesso | Grande · risco alto | — | ✅ Concluído — H1.3 "solicitar acesso" fechada via M05 (`POST /api/v1/leads` + `SolicitarAcessoPage`) |
| — | E15 · Gestão de Time (RBAC) | Médio · risco alto | — | ✅ Concluído |
| — | E17 · Painel Consolidado & Ranking | Grande · risco médio | — | ✅ Concluído |
| 1 | **E14 · Financeiro & DRE** | Grande · risco alto | 6d | ✅ Concluído |
| 2 | E13 · Comercial & Vendas | Grande | 6d + ~1d (H1.3) | ✅ Concluído — backend (90/90 testes) + `revisor-seguranca` (M1/M2/L2/L3 corrigidos) + frontend (dashboard/funil/ranking) + E2E (17/17, `comercial.spec.ts`) |
| 3 | E11 · Gestão Admin (mentorias ind./grupo, curadoria, eventos) + E5 · Mentorias & Atas + **diferencial de IA** (transcrição de áudio → rascunho de ata) | Grande | 6d + ~2-3d da integração de IA | ⬜ Blueprint pendente — decidir provedor (Whisper API) e custo por uso antes |
| 4 | Google OAuth (fast-follow do E1) | Pequeno | 1.5d | ⬜ Cai na mesma máquina de sessão/RBAC já pronta |
| 5 | E2 · Dashboard do Mentorado | Médio | 3.5d | ⬜ |
| 6 | E3 · Metas Estratégicas | Médio | 3.5d | ⬜ |
| 7 | E4 · Tarefas & Agenda | Médio | 3.5d | ⬜ Backend parcial já existe (`Encaminhamento`, peso 1/2, usado pelo E17) |
| 8 | E6 · Materiais & Dicas do Brayan | Médio | 3.5d | ⬜ |
| 9 | E7 · Eventos & Inscrições | Médio | 3.5d | ⬜ |
| 10 | E8 · Loja SAW (catálogo, carrinho, checkout, gateway) | Grande · risco alto | 6d | ⬜ `revisor-seguranca` obrigatório, mesmo tratamento do Auth |
| 11 | E9 · Perfil & Gamificação | Médio | 3.5d | ⬜ |
| 12 | E10 · Painel Administrativo & Métricas (parte além do E17, já pronto) | Médio | 3.5d | ⬜ |
| 13 | E16 · Avisos & Notificações (transversal) | Pequeno | 1.5d | ⬜ |
| — | **Fase 5 · Homologação** (smoke test via Docker, validação humana E2E, revisão final de segurança, deploy Coolify, **pass transversal de `pgcrypto` nas colunas sensíveis** — ver notas em M04 e M05) | — | 2d | ⬜ |

**Total restante (peso somado): ≈ 60 dias de engenharia.** Responsividade mobile fica **fora
deste pipeline** por decisão explícita do cliente (07/07/2026) — retorna como requisito só
depois do MVP no ar.

### Por que essa ordem
Segue a prioridade definida em reunião com o cliente (`CLAUDE.md`): núcleo do back-office
(E13/E14, já com E15 concluído) antes dos módulos do mentorado, e dentro do back-office o
módulo de maior risco entra primeiro (mesma lógica que já puxou E15 e agora puxa E14 antes de
E13). E11+E5+IA vem logo em seguida por ser o diferencial que o cliente pediu explicitamente
pra fechar negócio — não fica esperando todos os módulos do mentorado. Loja (E8) é o único
módulo de mentorado tratado como alto risco (gateway de pagamento), mas mantém sua posição
natural na sequência do núcleo do mentorado em vez de ser antecipado, já que depende de
Assinatura/Perfil (E9) existirem antes de fazer sentido ter carrinho/checkout.
