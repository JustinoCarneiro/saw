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
| **M06** | **E11 · Gestão Admin + E5 · Mentorias & Atas (lado Admin) + diferencial de IA** | **Grande** | **6d + ~2-3d (IA)** | **H11.1–H11.4 + H5.2 (dado, sem tela)** |
| **M07** | **Google OAuth (fast-follow do E1)** | **Pequeno** | **1.5d** | **H1.2** |
| **M08** | **E2 · Dashboard do Mentorado** | **Médio** | **3.5d** | **H2.1–H2.3** |
| **M09** | **E3 · Metas Estratégicas** | **Médio** | **3.5d** | **H3.1–H3.3** |
| **M10** | **E4 · Tarefas & Agenda** | **Médio** | **3.5d** | **H4.1–H4.5** |
| **M11** | **E6 · Materiais & Dicas do Brayan** | **Médio** | **3.5d** | **H6.1–H6.3** |
| **M12** | **E5 · Mentorias & Atas (lado mentorado)** | **Médio** | **4d** | **H5.1–H5.3** |
| **M13** | **E7 · Eventos & Inscrições (lado mentorado)** | **Médio** | **4.5d** | **H7.1–H7.3** |
| **M14** | **E8 · Loja SAW (catálogo, carrinho, checkout, gateway)** | **Grande · risco alto** | **6d** | **H8.1–H8.4** |
| **M15** | **E9 · Perfil & Gamificação** | **Médio** | **3.5d** | **H9.1–H9.3** |
| **M16** | **E10 · Painel Administrativo & Métricas** | **Médio** | **3.5d** | **H10.1–H10.3** |

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

### M06 — E11 · Gestão Admin + E5 · Mentorias & Atas + diferencial de IA

**Por que Grande:**
- **Complexidade:** três recursos administrativos novos numa leva só (Mentoria com máquina de
  estado própria e suporte a grupo via M:N, Conteúdo com controle de acesso por plano, Evento com
  sua própria máquina de estado), **mais** uma classe de funcionalidade que não existe em nenhum
  outro módulo do sistema até aqui: um pipeline assíncrono de IA (upload de áudio → transcrição →
  resumo estruturado → revisão humana → publicação).
- **Risco não listado no `CLAUDE.md` mas real** (mesmo raciocínio do M05 para `/leads`): este é o
  primeiro fluxo do sistema que envia dado do negócio do cliente (áudio de mentoria, potencialmente
  com informação sensível do restaurante do mentorado) pra **duas APIs de terceiros** (Whisper,
  Claude). Não está marcado "risco alto" no `CLAUDE.md`, mas o vetor de dado saindo da VPS pra
  fora merece `revisor-seguranca` antes de fechar — validação de tipo/tamanho de upload, timeout e
  tratamento de falha do provedor externo, e confirmação de que a transcrição/áudio ficam sob o
  mesmo guarda-chuva de backup do Postgres (não um volume solto sem cobertura).

**Suposições assumidas pra este Blueprint seguir adiante** (`docs/spec.md` § Suposições já lista
duas relacionadas, ainda não confirmadas pela SAW; a terceira é nova, não coberta no spec.md):
- **#3 do spec.md (mentoria em grupo):** sem confirmação de cliente, assumido **sem limite superior**
  de participantes por grupo, e **uma única ata por mentoria**, visível a todos os mentorados do
  grupo (não há ata por pessoa). Ajustar se a SAW definir uma regra diferente.
- **#5 do spec.md (integração de agenda):** campo genérico (`linkOnline` + `local` opcional), não
  travado só em Google Meet — o `CLAUDE.md` cita Meet como a integração do MVP, mas o formulário de
  criação de mentoria (H11.2) já pede "plataforma" como campo livre, então o modelo não trava nisso.
- **Nova suposição — dono de área de Eventos no RBAC (E15):** `CLAUDE.md` define Comercial,
  Marketing (conteúdos/marketing) e Gestão de Performance (Mentorados/Mentorias/Conteúdos/Painel
  Consolidado), mas não diz quem administra Eventos. Default assumido: **`Modulo.CONTEUDOS`**, o
  mesmo módulo de Marketing (eventos como atividade de marketing/growth), em vez de criar um
  `Modulo.EVENTOS` novo sem necessidade comprovada — mais fácil remover um agrupamento depois do que
  destrinchar RBAC espalhado por várias telas. Ajustar `AreaModuloMatrix` se a SAW quiser separar.

**Fechamento de pendência do M05:** `Lead.status=FECHADO` registra a venda pra métrica, mas
propositalmente **não cria a conta de login do mentorado** (nota do M05). H11.1 ("gerenciar
mentorados por plano e status") é onde isso se fecha: `POST /admin/mentorados/a-partir-do-lead/{leadId}`
cria o `Mentorado` (+ `Usuario` com `Perfil.MENTORADO`) a partir de um lead fechado, e seta
`Lead.mentoradoId` (nova coluna) pra rastrear a origem — sem isso, não haveria como saber quais
mentorados vieram de qual lead depois que o funil já rodou. `Mentorado` também ganha uma coluna
`status` (`ATIVO`/`INATIVO`) que hoje não existe — H11.1 pede filtro por status e a entidade atual
não tem esse campo.

**IA — decisões desta leva (conversa com Marcos, 2026-07-08):**
- **Transcrição:** Whisper API (já cotado no `CLAUDE.md` § Diferenciais do MVP).
- **Geração do rascunho** (resumo + encaminhamentos sugeridos): **Claude Sonnet 5**
  (`claude-sonnet-5`), com saída estruturada (tool use/JSON) — escolhido por custo/latência/qualidade
  em extração estruturada sobre um transcript; a tarefa (resumir + listar encaminhamentos) não tem
  complexidade de raciocínio que justifique Opus.
- **Processamento assíncrono via `@Async` + `Executor` dedicado**, não fila pesada — mesmo
  raciocínio da decisão de stack do `CLAUDE.md` (`@Scheduled`/jobs em vez de fila de alto volume):
  aqui o gatilho é por upload (evento), não periódico, mas a razão de não introduzir
  RabbitMQ/SQS pra um volume baixo (uma mentoria de cada vez, não é caso de uso de alto throughput)
  é a mesma. Frontend faz **polling** do `statusProcessamento` — não há WebSocket no stack.
- **Revisão humana é obrigatória antes de qualquer efeito em métrica:** a IA nunca escreve direto
  em `Encaminhamento` — ela gera `AtaEncaminhamentoSugerido` (rascunho), e só na publicação da ata
  (`POST .../publicar`) as sugestões aceitas viram `Encaminhamento` de verdade, com `mentoriaId`
  setado. É só nesse momento que passam a contar pro ranking do E17 — evita a IA escrever direto
  numa métrica que já vira ranking/desempenho do time (mesmo cuidado do M05 em não deixar `Lead`
  registrar venda sem `planoFechado`/L2).
- **Custo por uso:** a considerar no orçamento de infra (`CLAUDE.md` já sinaliza isso) — cada
  mentoria com áudio gera custo variável (Whisper por minuto de áudio + Claude por tokens do
  resumo); não precificado aqui, só a arquitetura que gera esse custo.
- **Armazenamento do áudio:** disco da própria VPS (volume Docker, ex. `/data/audios`) pro MVP —
  escala de 10-15 usuários não justifica object storage dedicado agora (mesmo raciocínio do
  `CLAUDE.md` § Hospedagem de não pagar por infra que a escala atual não pede). Entra no mesmo pass
  de revisão de backup da Fase 5: é dado do negócio do cliente, precisa estar coberto pelo mesmo
  backup do Postgres, não um volume solto sem retenção. **Ainda não está no `docker-compose.full.yml`**
  (volume dedicado) — follow-up de infra antes do deploy real, não bloqueia o MVP local.

**Achados da revisão de segurança (`revisor-seguranca`), todos corrigidos:**
- **Alto:** chamadas à Whisper API e à Claude API sem timeout — uma API lenta/instável travava a
  thread do `ataProcessamentoExecutor` (pool de só 2-4 threads) indefinidamente, deixando a ata
  presa em `PROCESSANDO` pra sempre (`iniciarProcessamento()`/`publicar()` bloqueiam nesse estado).
  Corrigido com `SimpleClientHttpRequestFactory` (connect timeout 10s, read timeout 3min na Whisper
  — áudio+transcrição podem levar minutos — e 90s na Claude).
- **Médio:** `POST .../ata/audio` sem rate limit — diferente de `/leads` (M05), cada chamada dispara
  uma requisição paga em duas APIs de terceiros, e o próprio design permite reenvio (retry após
  FALHA, regravar após CONCLUIDO) sem limite. Corrigido com `AtaAudioRateLimitFilter` (mesmo padrão
  Redis do `LeadRateLimitFilter`, mas por usuário autenticado, não por IP — este endpoint já exige
  login): 10 uploads/hora.
- **Médio:** upload de áudio sem allow-list de extensão/content-type nem `max-file-size`
  configurado — o nome do arquivo do cliente virava extensão do arquivo em disco sem checagem
  (podia ser `.php`/`.html`), e o binário era reencaminhado pra Whisper API sem validar
  content-type. Corrigido com allow-list de extensões (`.mp3/.wav/.m4a/.ogg/.webm/.aac/.flac`) +
  checagem de `content-type` (`audio/*`) em `AudioStorageService`, e
  `spring.servlet.multipart.max-file-size=150MB` em `application.yml`.
- **Baixo:** `PATCH .../ata/sugestoes/{id}` continuava aceitando edição depois da ata `PUBLICADA`,
  divergindo do que já foi materializado em `Encaminhamento` — inconsistência de trilha de auditoria
  num documento que deveria ser imutável após publicado. Corrigido reusando `Ata.exigirRascunho()`
  (mesma checagem que já protegia `editarResumo()`/`publicar()`).

**Bugs reais achados na verificação ao vivo (curl + navegador), todos corrigidos com teste de
regressão:**
- Filtro opcional de texto nulo em JPQL (`MentoradoRepository.buscarComFiltro`, busca por nome) —
  com `busca=null`, o Postgres não conseguia inferir o tipo do parâmetro dentro de
  `CONCAT('%', :busca, '%')` e escolhia `bytea` em vez de `text` ("function lower(bytea) does not
  exist"), derrubando o boot inteiro da aplicação (o `DemoDataSeeder` chama esse método). Corrigido
  com `CAST(:busca AS string)`.
- Mesma classe de bug em `MentoriaRepository.buscarComFiltro` (filtro opcional de `Instant`
  nulo, `de`/`ate`) — dessa vez o `CAST` não bastou (`cannot cast type bytea to timestamp without
  time zone`, um comportamento diferente do Hibernate 6 pra parâmetros temporais dentro de função
  JPQL). Resolvido filtrando `de`/`ate` em memória no `MentoriaService` (mesmo padrão já usado em
  `LancamentoService`/financeiro — dataset pequeno, endpoint admin autenticado).
- `@Lob` em `String` (`Ata.resumo`/`transcricao`) mapeia pra `oid` (large object) no Postgres, não
  `text` — a migration criava as colunas como `TEXT`, e a validação do schema do Hibernate falhava
  no boot. Corrigido trocando `@Lob` por `@Column(columnDefinition = "text")`.
- Coluna `criado_em` faltando na migration de `ata_encaminhamento_sugerido` (a entidade herda de
  `BaseEntity`, que exige `criado_em`+`versao`) — mesma falha de boot por schema-validation.
- Overflow de e-mail na tela de Mentorados (mesmo bug já corrigido no M05/Comercial): e-mail longo
  sem espaços estourava a largura da coluna do grid e sobrepunha a coluna de Plano. Corrigido com
  `overflow-wrap: anywhere` no CSS module da coluna.

## Modelagem de banco (M06)

```sql
-- Máquina de estado (CLAUDE.md): Agendada -> Confirmada -> Realizada (gera ata) | desvio -> Cancelada
CREATE TABLE mentoria (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo            VARCHAR(20) NOT NULL,                      -- INDIVIDUAL | GRUPO
    mentor_id       UUID NOT NULL REFERENCES colaborador(id),
    data_hora       TIMESTAMP NOT NULL,
    duracao_min     INT NOT NULL,
    link_online     VARCHAR(500),
    local           VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'AGENDADA',
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_mentoria_status CHECK (status IN ('AGENDADA','CONFIRMADA','REALIZADA','CANCELADA')),
    CONSTRAINT chk_mentoria_tipo CHECK (tipo IN ('INDIVIDUAL','GRUPO'))
);

-- M:N de propósito mesmo pra INDIVIDUAL (1 linha só) — evita duas modelagens paralelas p/ solo x grupo.
CREATE TABLE mentoria_mentorado (
    mentoria_id     UUID NOT NULL REFERENCES mentoria(id) ON DELETE CASCADE,
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id),
    PRIMARY KEY (mentoria_id, mentorado_id)
);

-- 1:1 com mentoria — nasce (vazia) quando mentoria muda pra REALIZADA.
CREATE TABLE ata (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentoria_id           UUID NOT NULL UNIQUE REFERENCES mentoria(id),
    audio_url             VARCHAR(500),
    transcricao           TEXT,
    resumo                TEXT,
    status_processamento  VARCHAR(20) NOT NULL DEFAULT 'SEM_AUDIO',  -- SEM_AUDIO|PROCESSANDO|CONCLUIDO|FALHA
    status                VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',    -- RASCUNHO|PUBLICADA
    publicada_em          TIMESTAMP,
    criado_em             TIMESTAMP NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_ata_status_proc CHECK (status_processamento IN ('SEM_AUDIO','PROCESSANDO','CONCLUIDO','FALHA')),
    CONSTRAINT chk_ata_status CHECK (status IN ('RASCUNHO','PUBLICADA'))
);

-- Rascunho gerado pela IA — só materializa em `encaminhamento` de verdade na publicação (ver nota de IA acima).
CREATE TABLE ata_encaminhamento_sugerido (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ata_id          UUID NOT NULL REFERENCES ata(id) ON DELETE CASCADE,
    titulo          VARCHAR(255) NOT NULL,
    peso_sugerido   SMALLINT NOT NULL DEFAULT 1,
    aceito          BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_peso_sugerido CHECK (peso_sugerido IN (1,2))
);

-- Nullable de propósito: encaminhamentos já existentes (seed/E4 manual) não têm mentoria de origem.
ALTER TABLE encaminhamento ADD COLUMN mentoria_id UUID REFERENCES mentoria(id);

CREATE TABLE conteudo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo          VARCHAR(255) NOT NULL,
    tipo            VARCHAR(20) NOT NULL,               -- DOCUMENTO|VIDEO|PLANILHA|APRESENTACAO|OUTRO
    url             VARCHAR(500) NOT NULL,
    plano_minimo    VARCHAR(20) NOT NULL DEFAULT 'GRATUITO',
    publicado       BOOLEAN NOT NULL DEFAULT false,
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_conteudo_plano CHECK (plano_minimo IN ('GRATUITO','BASICO','ESSENCIAL','PROFISSIONAL'))
);

CREATE TABLE mentoria_material_recomendado (
    mentoria_id     UUID NOT NULL REFERENCES mentoria(id) ON DELETE CASCADE,
    conteudo_id     UUID NOT NULL REFERENCES conteudo(id),
    PRIMARY KEY (mentoria_id, conteudo_id)
);

-- Máquina de estado própria (H11.4), independente da de mentoria.
CREATE TABLE evento (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo          VARCHAR(255) NOT NULL,
    tipo            VARCHAR(20) NOT NULL,               -- AO_VIVO|PRESENCIAL
    tema            VARCHAR(255),
    data_hora       TIMESTAMP NOT NULL,
    local           VARCHAR(255),
    link_online     VARCHAR(500),
    vagas           INT,                                 -- null = ilimitado
    status          VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_evento_status CHECK (status IN ('PROGRAMADO','AO_VIVO','REALIZADO','CANCELADO')),
    CONSTRAINT chk_evento_tipo CHECK (tipo IN ('AO_VIVO','PRESENCIAL'))
);

ALTER TABLE mentorado ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE mentorado ADD CONSTRAINT chk_mentorado_status CHECK (status IN ('ATIVO','INATIVO'));
ALTER TABLE lead ADD COLUMN mentorado_id UUID REFERENCES mentorado(id);
```

## Contratos de API (M06)

### Mentorados (`@RequiresModulo(Modulo.MENTORADOS)`) — fecha H11.1
```jsonc
GET /api/v1/admin/mentorados?plano=&status=&busca=      // MentoradoResponse[]
PATCH /api/v1/admin/mentorados/{id}                       // editar plano/status
POST /api/v1/admin/mentorados/a-partir-do-lead/{leadId}    // cria Mentorado+Usuario a partir de Lead FECHADO
// Response 201
{ "id": "uuid", "nome": "Maria Souza", "plano": "ESSENCIAL", "status": "ATIVO" }
```

### Mentorias (`@RequiresModulo(Modulo.MENTORADOS)`) — fecha H11.2
```jsonc
POST /api/v1/admin/mentorias
{ "tipo": "INDIVIDUAL", "mentoradoIds": ["uuid"], "mentorId": "uuid",
  "dataHora": "2026-07-15T14:00:00Z", "duracaoMin": 60,
  "linkOnline": "https://meet.google.com/abc-defg-hij", "local": null }
// Response 201: MentoriaResponse

GET /api/v1/admin/mentorias?status=&de=&ate=     // agenda/histórico (fecha H5.1/H5.2 no back)
// de/ate filtram em memória, não em SQL — achado ao vivo, ver nota de bugs corrigidos acima.
GET /api/v1/admin/mentorias/mentores             // colaboradores de GESTAO_PERFORMANCE (seletor do form)

PATCH /api/v1/admin/mentorias/{id}/status         // CONFIRMADA ou CANCELADA
{ "novoStatus": "CONFIRMADA" }

POST /api/v1/admin/mentorias/{id}/realizar
// REALIZADA não passa por /status — cria a Ata (vazia) atomicamente com a transição, por isso
// tem endpoint próprio (ver AtaService.realizarMentoria). Response: AtaResponse.
```

### Ata (mesmo `RequiresModulo`) — fecha H5.2 + diferencial de IA
```jsonc
GET /api/v1/admin/mentorias/{id}/ata

POST /api/v1/admin/mentorias/{id}/ata/audio     // multipart/form-data (campo "arquivo"), dispara o pipeline assíncrono
// Response 202: AtaResponse com statusProcessamento=PROCESSANDO
// Validado: allow-list de extensão (.mp3/.wav/.m4a/.ogg/.webm/.aac/.flac) + content-type audio/*,
// rate limit de 10 uploads/hora por usuário (achados M/M da revisão de segurança, ver acima).

PATCH /api/v1/admin/mentorias/{id}/ata            // editar resumo manualmente (revisão humana)
{ "resumo": "..." }

PATCH /api/v1/admin/mentorias/{id}/ata/sugestoes/{sugestaoId}
{ "titulo": "Atualizar ficha técnica", "pesoSugerido": 2, "aceito": true }

POST /api/v1/admin/mentorias/{id}/ata/publicar
// RASCUNHO -> PUBLICADA; materializa sugestões aceitas em Encaminhamento reais.
// 409 se statusProcessamento=PROCESSANDO (não publica no meio do processamento).
```

### Conteúdos (`@RequiresModulo(Modulo.CONTEUDOS)`) — fecha H11.3
```jsonc
POST/GET/PATCH /api/v1/admin/conteudos     // CRUD + publicar/despublicar (plano_minimo controla acesso)
```

### Eventos (`@RequiresModulo(Modulo.CONTEUDOS)` — default assumido, ver Suposição acima) — fecha H11.4
```jsonc
POST/GET/PATCH /api/v1/admin/eventos       // CRUD + transição PROGRAMADO→AO_VIVO→REALIZADO (ou CANCELADO)
```

## Rastreabilidade história ↔ módulo (M06)

| História | Cobertura |
|---|---|
| H11.1 — gerenciar mentorados por plano/status | `GET/PATCH /admin/mentorados`, `POST .../a-partir-do-lead/{leadId}` (fecha a pendência do M05) |
| H11.2 — criar mentoria individual/grupo | `POST /admin/mentorias` |
| H11.3 — gerir biblioteca de conteúdos | `POST/GET/PATCH /admin/conteudos` |
| H11.4 — gerir eventos | `POST/GET/PATCH /admin/eventos` |
| H5.2 — histórico e ata da mentoria | `GET /admin/mentorias/{id}/ata` |
| Diferencial de IA (`CLAUDE.md` § Diferenciais do MVP — não numerado como história em `spec.md`, extensão de H5.2) | `POST .../ata/audio`, `PATCH .../ata/sugestoes/{id}`, `POST .../ata/publicar` |

**H5.1 (entrar na reunião) e H5.3 (.ics) são histórias do mentorado, não do Admin** — o dado
(`linkOnline`, `dataHora`) já nasce pronto nesta leva, mas a **tela** do mentorado só existe quando
os módulos do mentorado entrarem no pipeline (item 5+ abaixo). Mesmo padrão já usado pelo E17
rodando sobre dado seed antes do E4 estar completo: não é um buraco desta leva, é ordem de
construção — back-office e dado antes, tela do mentorado depois, por decisão do cliente
(`CLAUDE.md` § MVP · Prioridade de construção).

**Status: ✅ M06 concluído** (2026-07-09) — backend (137/137 testes: entidades/serviços/controllers
+ `LeadRepositoryTest`-style `@DataJpaTest` pra `LeadRepository`/`AudioStorageService`), 4 achados
do `revisor-seguranca` corrigidos, 5 bugs reais achados na verificação ao vivo corrigidos com teste
de regressão, frontend completo (`MentoradosShell`, `ConteudosShell` + 5 páginas novas) e E2E
(`mentorados.spec.ts`, 4 testes cobrindo o fluxo ponta a ponta lead→mentorado→mentoria→ata
publicada, mais Conteúdos/Eventos/RBAC) — 21/21 verde na suíte completa. **Pendência explícita**:
o pipeline de IA foi verificado só até a borda (falha limpa e clara sem `OPENAI_API_KEY`/
`ANTHROPIC_API_KEY` configuradas) — a chamada real a Whisper/Claude nunca rodou de ponta a ponta
por falta de credenciais neste ambiente; validar com chaves reais antes de qualquer demo que
dependa da transcrição funcionar de fato.

### M07 — Google OAuth (fast-follow do E1)

**Por que Pequeno:** cai na máquina de sessão/RBAC já pronta (`SecurityContextRepository`,
`AppUserPrincipal`, `CustomUserDetailsService`) — não precisa de estado novo, só um segundo jeito
de chegar no mesmo `AppUserPrincipal`.

**H1.2 do `spec.md`:** "Como mentorado, quero entrar com o Google... se for o 1º acesso, minha
conta é vinculada." **Decisão de escopo:** contas nascem por ação do Admin (E11, `POST
.../a-partir-do-lead`), não por auto-cadastro — então "vincular" aqui significa **casar pelo
e-mail com uma conta que já existe**, nunca criar uma nova. Login Google com e-mail sem `Usuario`
correspondente é rejeitado com mensagem apontando pra "Solicitar acesso" (H1.3, já existe desde o
M05), não silenciosamente cria conta. Reaproveita `CustomUserDetailsService.loadUserByUsername`
tal qual — mesmo cálculo de authorities pra ADMIN e MENTORADO, zero lógica duplicada.

**Suposição, não coberta pelo `spec.md`:** a história é escrita "como mentorado", mas nada impede
Admin de usar Google também (o lookup por e-mail é agnóstico de perfil) — implementado pros dois.
Pós-login: ADMIN cai em `/admin` (o `LoginPage` já resolve o primeiro módulo permitido); MENTORADO
autentica com sucesso mas **não tem área própria ainda** (E2+ não construído) — mesmo "buraco
esperado" já documentado no M06 pra H5.1/H5.3, não uma falha desta leva.

**Decisão de arquitetura — sem credencial, sem risco pro que já funciona:** declarar
`spring.security.oauth2.client.registration.google.client-id` vazio no `application.yml` faz o
Spring Boot falhar o *boot* (`ClientRegistration.validate()` exige client-id não-vazio assim que a
propriedade existe). Por isso o `ClientRegistrationRepository` é montado **programaticamente**
(`CommonOAuth2Provider.GOOGLE` + client-id/secret vindos de env) só dentro de um `if` no
`SecurityConfig`, e `.oauth2Login(...)` só entra no filter chain quando as duas envs
(`GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`) existem. Sem elas, `/oauth2/authorization/google`
não tem nenhum filtro do Spring Security registrado pra tratá-la — cai no mesmo comportamento
padrão de qualquer rota inexistente da aplicação (401 genérico via `JsonAuthEntryPoint`, não um
404 "puro" do container, porque o forward pro `/error` volta a passar pelo filter chain e cai em
`anyRequest().authenticated()` — comportamento preexistente, não específico do OAuth2). Login
e-mail/senha continua idêntico, nada quebra, nenhuma etapa do fluxo OAuth2 fica exposta.
`GET /api/v1/auth/oauth2-config` (público) expõe se está habilitado, pro frontend só mostrar o
botão "Entrar com Google" quando fizer sentido — sem botão morto em dev/demo sem credencial.

**Pendência explícita, mesma classe do M06:** sem app OAuth registrado no Google Cloud Console
(Marcos confirmou que ainda não tem), o fluxo real (redirect pro Google, consentimento, callback)
nunca roda de ponta a ponta neste ambiente. Verificado até a borda: lookup de e-mail
(`GoogleOAuth2UserService`, testado com atributos OAuth2 mockados), rejeição de e-mail não
verificado, rejeição de e-mail sem conta, e que o boot/login e-mail-senha não quebram com ou sem
as envs configuradas. Validar o redirect real quando houver credencial.

**Achado da revisão de segurança (`revisor-seguranca`), corrigido:** o código de erro
`conta_nao_encontrada` (rejeição de e-mail do Google sem `Usuario` correspondente) era um oráculo
de enumeração de contas — violava o mesmo princípio do H1.1 ("mensagem de erro clara sem revelar
qual campo falhou") que o login e-mail/senha já respeita via `AuthFailureHandler` +
`CustomUserDetailsService` (mensagem genérica "Credenciais inválidas" tanto pra `Usuario` quanto
`Colaborador`/`Mentorado` ausente). Corrigido: código renomeado pra `login_nao_permitido` com
mensagem neutra ("Não foi possível concluir o login com esta conta Google"), sem confirmar nem
negar existência de conta — o link "Solicitar acesso" já fica sempre visível na tela de login
independente do motivo da rejeição. `email_nao_verificado` ficou como estava (não revela nada
sobre a existência de conta SAW HUB, só o estado de verificação no próprio Google).

## Contratos de API (M07)

```jsonc
GET /api/v1/auth/oauth2-config          // público
{ "googleEnabled": false }

GET /oauth2/authorization/google        // só existe se googleEnabled=true; gerenciado pelo Spring Security
GET /login/oauth2/code/google           // callback do Google; gerenciado pelo Spring Security
// Sucesso: redireciona pro frontend autenticado (cookie de sessão já setado no redirect).
// Falha (e-mail não verificado: código email_nao_verificado; sem Usuario correspondente: código
// login_nao_permitido, mensagem genérica de propósito — ver achado do revisor-seguranca abaixo):
// redireciona pro /login?erroOAuth=<código>, frontend traduz pra mensagem amigável.
```

## Rastreabilidade história ↔ módulo (M07)

| História | Cobertura |
|---|---|
| H1.2 — entrar com Google | `GET /oauth2/authorization/google` + `GoogleOAuth2UserService` (casa por e-mail com `Usuario` existente) |

**Status: ✅ M07 concluído** (2026-07-09) — backend (141/141 testes, incluindo `GoogleOAuth2UserServiceTest`
e `contextLoads` provando que o boot não quebra sem credenciais Google configuradas), 2 achados do
`revisor-seguranca` corrigidos (oráculo de enumeração de contas no código de erro
`conta_nao_encontrada` → `login_nao_permitido`, mais esta própria nota de achado que estava
pendurada sem conteúdo — ambos detalhados na seção do Blueprint acima), frontend (botão condicional
+ tradução de erro + mensagem de "área em construção" pro Mentorado), verificação ao vivo via curl
(boot sem credenciais, `oauth2-config` retornando `{"googleEnabled":false}`, login e-mail/senha
íntegro, `/oauth2/authorization/google` corretamente inacessível sem credencial) e E2E
(`google-oauth.spec.ts`, 3 testes) — 24/24 verde na suíte completa. **Pendência explícita, mesma
classe do M06**: sem app OAuth registrado no Google Cloud Console, o fluxo real (redirect →
consentimento → callback) nunca rodou de ponta a ponta neste ambiente; validar com credenciais
reais antes de qualquer demo que dependa do login Google funcionar de fato.

### M08 — E2 · Dashboard do Mentorado

**Por que Médio:** primeira tela do lado Mentorado de verdade (até aqui só existia um placeholder
de "área em construção" pós-login, M07). Não é CRUD — é leitura agregada de dados que já existem
(`Mentorado`, `Encaminhamento`, `Mentoria`, `Conteudo`), então não tem a complexidade de um módulo
"Grande" como E13/E14/E11, mas cria pela primeira vez a árvore de rotas `/mentorado` no frontend
(shell, guarda de perfil, navegação) — não é "Pequeno" como o M07 porque isso é esqueleto novo, não
um fast-follow sobre algo que já existe.

**Modelagem de banco:** nenhuma entidade nova. H2.1–H2.3 são inteiramente supridas por dado que já
existe: `Mentorado` (nome/plano, já usado no E11), `Encaminhamento` (tarefas, já usado no E4 parcial
e no E17), `Mentoria` (compromissos, já usado no E5/E11) e `Conteudo` (dica em destaque, já usado no
E6 parcial/E11). Só entram métodos novos de leitura nos repositórios existentes — nenhuma tabela,
nenhuma migração Flyway.

**H2.1 (visão geral) — decisões de escopo:**
- **Tarefas abertas** e **evolução geral (%)**: dado real, sem suposição. Evolução geral reusa a
  *mesma* fórmula de peso-concluído/peso-total já usada em `MentoradoConsolidadoResponse.from()`
  (E17) — extraída pra um helper compartilhado (`PesoProgressoCalculator` ou equivalente em
  `com.sawhub.hub.mentorado`) especificamente pra impedir que o Dashboard do mentorado e o Painel
  Consolidado do Admin um dia mostrem números diferentes pro mesmo mentorado por causa de duas
  implementações da mesma conta divergindo ao longo do tempo — não é abstração prematura, é a mesma
  conta usada em dois lugares.
- **Meta semanal (%)**: **suposição, não coberta pelo `spec.md`** — E3 · Metas Estratégicas não
  existe ainda (nenhuma entidade `Meta`), então este campo volta `null`/omitido nesta leva, com o
  frontend mostrando um estado "Metas ainda não disponíveis" em vez de inventar um número. Mesmo
  padrão "buraco esperado, não falha desta leva" já usado no M06 (H5.1/H5.3) e M07 (mentee sem área
  própria). Volta a ser um número real quando o M09/E3 (próximo da fila) entrar.

**H2.2 (compromissos) — decisão de escopo:** "mentorias/visitas/eventos futuros" do `spec.md` vira,
nesta leva, **só Mentorias** onde o mentorado logado é membro (`MEMBER OF`, já que `Mentoria` é M:N
com `Mentorado` mesmo pra tipo INDIVIDUAL — ver nota do M06), status `AGENDADA`/`CONFIRMADA`, data
futura. "Visitas" não tem modelagem em lugar nenhum do sistema (não é um gap desta leva, é uma
suposição do próprio `spec.md`, fora de escopo até virar história própria). Eventos ficam de fora
porque E7 · Eventos & Inscrições ainda não construiu o vínculo mentorado↔evento (M06 só entregou o
CRUD administrativo do Evento, sem inscrição) — mostrar "todo evento futuro" pra todo mentorado
seria inventar uma inscrição implícita que não existe. Filtro de data feito em Java (mesmo padrão
já usado em `MentoriaService`/`LancamentoService` — dataset pequeno por mentorado, não JPQL, pelo
mesmo problema de inferência de tipo do Postgres com `Instant` achado no M06).

**H2.3 (avisos + dica do Brayan) — decisões de escopo:**
- **Avisos**: E16 · Avisos & Notificações (transversal) não existe — nenhuma entidade `Aviso`.
  Lista sempre vazia nesta leva, com estado "Nenhum aviso no momento" no frontend. Não é um buraco
  desta leva: E16 é um épico transversal próprio, sem prioridade definida ainda no pipeline.
- **Dica do Brayan em destaque**: **suposição, não coberta pelo `spec.md`** — E6 · Materiais &
  Dicas do Brayan não existe como épico próprio ainda; o `spec.md` não diz como um conteúdo vira "a
  dica em destaque" (não existe um campo "destaque"). Proxy adotado: o `Conteudo` (já existe desde
  o M06) mais recente com `tipo=VIDEO`, `publicado=true` e `planoMinimo` dentro do plano do
  mentorado (`Plano` é um enum ordenado — `planoMinimo.ordinal() <= mentorado.plano.ordinal()`).
  Se não houver nenhum, estado vazio. Revisitar quando E6 definir curadoria própria (campo
  `destaque` explícito seria o caminho natural, mas isso é decisão do Blueprint do E6, não deste).

**Frontend — primeira rota `/mentorado`:** `MentoradoShell` novo, mesmo padrão do `AdminShell`
(guarda de `perfil`, mas checando `MENTORADO` em vez de `ADMIN` — perfil Mentorado não tem conceito
de RBAC por área, então não reusa `Sidebar`/`Modulo`, que são exclusivos do Admin/E15). Sem
navegação multi-item ainda — só existe a página de Dashboard nesta leva; a navegação cresce quando
E3/E4 entrarem (mesma ordem já definida em `CLAUDE.md` § MVP: Dashboard → Metas/Tarefas → Mentorias
→ resto). `LoginPage.tsx` troca a mensagem placeholder "área em construção" (M06/M07) por navegação
real pra `/mentorado` — fecha essa pendência que vinha sendo documentada desde o M06.

## Contratos de API (M08)

```jsonc
GET /api/v1/mentorado/dashboard        // hasRole("MENTORADO"), já coberto pelo SecurityConfig
{
  "nome": "Maria Silva",
  "evolucaoGeralPct": 62,
  "tarefasAbertas": 3,
  "metaSemanalPct": null,              // E3 não construído nesta leva — sempre null por ora
  "proximaReuniao": {                  // null se não houver nenhuma futura
    "id": "uuid",
    "tipo": "INDIVIDUAL",
    "dataHora": "2026-07-15T14:00:00Z",
    "linkOnline": "https://meet.google.com/...",
    "local": null
  },
  "compromissos": [                    // mesmo formato do item acima, ordenado por dataHora ASC
    { "id": "uuid", "tipo": "INDIVIDUAL", "dataHora": "2026-07-15T14:00:00Z", "linkOnline": "...", "local": null }
  ],
  "dicaDestaque": {                    // null se não houver Conteudo elegível
    "id": "uuid",
    "titulo": "Como montar sua ficha técnica",
    "url": "https://..."
  },
  "avisos": []                         // E16 não construído nesta leva — sempre vazio por ora
}
```

## Rastreabilidade história ↔ módulo (M08)

| História | Cobertura |
|---|---|
| H2.1 — visão geral (reunião, meta, tarefas, evolução) | `GET /mentorado/dashboard` (`evolucaoGeralPct`, `tarefasAbertas`, `metaSemanalPct`, `proximaReuniao`) |
| H2.2 — próximos compromissos | `GET /mentorado/dashboard` (`compromissos`, só Mentorias nesta leva — ver Suposição acima) |
| H2.3 — avisos + dica do Brayan | `GET /mentorado/dashboard` (`avisos` sempre vazio nesta leva; `dicaDestaque` via proxy em `Conteudo`) |

**Status: ✅ M08 concluído** (2026-07-09) — backend (152/152 testes, incluindo `ProgressoCalculatorTest`
e `MentoradoDashboardServiceTest` novos), `revisor-seguranca` sem achado bloqueante (isolamento por
tenant confirmado por análise de código: o id do mentorado usado em toda a agregação vem só de
`principal.getUsuarioId()`, nunca de request; RBAC `/api/v1/mentorado/**` intacto; sem risco de
injeção JPQL na query `MEMBER OF` nova — só 2 notas de robustez sem exploração possível, endereçadas
com comentário no código), frontend (`MentoradoShell` + `DashboardMentoradoPage`, primeira rota
`/mentorado` de verdade — fecha o placeholder "área em construção" documentado desde o M06),
verificação ao vivo via curl com 3 contas reais (Rafael/João/Admin: dado correto por mentorado,
403 pro Admin) e E2E (`dashboard-mentorado.spec.ts`, 5 testes) — 29/29 verde na suíte completa, sem
regressão. Sem pendência de credencial externa nesta leva (diferente de M06/M07) — módulo
inteiramente verificável neste ambiente.

### M09 — E3 · Metas Estratégicas

**Por que Médio:** primeira entidade nova desde o M06 (`Meta`, migração Flyway V6), mas é um único
CRUD self-service com máquina de estado simples (3 estados persistidos) — mesmo porte de
`Conteudo`/`Evento` no M06, não a complexidade de um módulo com múltiplos agregados como
Financeiro/Comercial.

**Achado ao inspecionar `design/prototipo/index.html` (mockup congelado):** a tela de Metas tem um
botão **"+ Nova meta"** — o mentorado cria e gerencia as próprias metas. **Suposição, não coberta
pelo `spec.md`:** H3.1–H3.3 só descrevem visualizar/filtrar/resumir, nenhuma história de criação;
Metas também não aparecem no escopo do E11 (Gestão Admin lista mentorados/mentorias/conteúdos/
eventos, não Metas). Adotado: Meta é **inteiramente self-service do mentorado** — criar, editar
(título/descrição/prazo/progresso), pausar, reativar, concluir — sem curadoria do Admin, ao
contrário de Conteúdos/Eventos. `progressoPct` é **editado manualmente** pelo mentorado; nada no
`spec.md` ou no mockup sugere um checklist de sub-itens ou cálculo automático.

**Máquina de estado (CLAUDE.md):** `Ativa {No prazo | Atenção | Atrasada} → Concluída` · desvio
`→ Pausada`. Só `ATIVA`, `CONCLUIDA`, `PAUSADA` são persistidos — o sub-status (`No prazo`/
`Atenção`/`Atrasada`) só existe enquanto `ATIVA` e é **calculado no response**, nunca gravado (mesmo
padrão do E17: status derivado de dado bruto, não uma coluna própria que pode ficar dessincronizada).
**Suposição de limiar a validar com o cliente** (não especificado em `spec.md` nem no mockup):
`Atrasada` = prazo já passou; `Atenção` = prazo a 7 dias ou menos; `No prazo` = caso contrário.

**Isolamento por tenant:** mesmo padrão do M08 — toda operação resolve o `Mentorado` a partir de
`principal.getUsuarioId()` (nunca de path/query), então create/edit/transição só operam sobre metas
do próprio mentorado autenticado (achado a confirmar explicitamente no `revisor-seguranca`: um
mentorado não pode editar/concluir a meta de outro só trocando o `{id}` na URL).

## Modelagem de banco (M09)

```sql
-- V6__metas.sql
CREATE TABLE meta (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id),
    titulo          VARCHAR(255) NOT NULL,
    descricao       VARCHAR(1000),
    prazo           DATE NOT NULL,
    progresso_pct   SMALLINT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    criado_em       TIMESTAMP NOT NULL DEFAULT now(),
    versao          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_meta_status CHECK (status IN ('ATIVA','CONCLUIDA','PAUSADA')),
    CONSTRAINT chk_meta_progresso CHECK (progresso_pct BETWEEN 0 AND 100)
);

CREATE INDEX idx_meta_mentorado ON meta(mentorado_id);
CREATE INDEX idx_meta_status ON meta(status);
```

## Contratos de API (M09)

```jsonc
POST /api/v1/mentorado/metas             // hasRole("MENTORADO")
{ "titulo": "Reduzir CMV em 5%", "descricao": "Renegociar 3 fornecedores principais", "prazo": "2026-09-30" }
// 201 -> MetaResponse (progressoPct=0, status=ATIVA)

GET /api/v1/mentorado/metas?status=ATIVA  // status opcional (ATIVA|CONCLUIDA|PAUSADA); omitido = Todas
[{
  "id": "uuid", "titulo": "Reduzir CMV em 5%", "descricao": "...",
  "prazo": "2026-09-30", "diasRestantes": 83, "progressoPct": 40,
  "status": "ATIVA", "subStatus": "NO_PRAZO"   // subStatus null quando status != ATIVA
}]

GET /api/v1/mentorado/metas/resumo        // H3.3 — sempre sobre TODAS as metas, ignora o filtro da tela
{ "conclusaoMediaPct": 20, "concluidas": 1, "noPrazo": 3, "atrasadas": 1 }

PUT /api/v1/mentorado/metas/{id}          // edita título/descrição/prazo/progresso — só ATIVA/PAUSADA
{ "titulo": "...", "descricao": "...", "prazo": "2026-10-15", "progressoPct": 55 }

PATCH /api/v1/mentorado/metas/{id}/status // { "novoStatus": "CONCLUIDA" | "PAUSADA" | "ATIVA" }
```

## Rastreabilidade história ↔ módulo (M09)

| História | Cobertura |
|---|---|
| H3.1 — ver metas com %, prazo, dias restantes, status | `GET /mentorado/metas` (`progressoPct`, `prazo`, `diasRestantes`, `status`/`subStatus`) |
| H3.2 — filtrar por Ativas/Concluídas/Pausadas/Todas | `GET /mentorado/metas?status=` |
| H3.3 — resumo geral | `GET /mentorado/metas/resumo` |

**Status: ✅ M09 concluído** (2026-07-09) — backend (165/165 testes, incluindo `MetaResponseTest` e
`MetaServiceTest` novos), primeira entidade nova desde o M06 (`Meta`, migração `V6__metas.sql`) —
um mismatch real de schema (`SMALLINT` na migração vs `Integer` puro na entidade) foi achado e
corrigido antes do commit, via `contextLoads`/schema-validation do Hibernate. `revisor-seguranca`
sem achado bloqueante — segundo módulo seguido com revisão limpa (M08 foi o primeiro), agora com o
risco maior de ser o primeiro módulo do Mentorado com caminhos de escrita (criar/editar/pausar/
reativar/concluir), não só leitura. Isolamento por tenant confirmado ponto a ponto: toda operação
resolve o dono via `principal.getUsuarioId()`, checagem de posse (`buscarDoUsuario`) cobre PUT e
PATCH sem exceção, 404 genérico (não 403) pra meta de outro mentorado. Frontend (`MetasPage` +
navegação Dashboard/Metas no `MentoradoShell`) e E2E (`metas.spec.ts`, 3 testes; achado um
test-data-collision real — mesma classe do M05 — corrigido com título único por execução +
`data-testid` opcional novo em `DataGridRow`) — 32/32 verde na suíte completa. Sem pendência de
credencial externa. **Pendência de produto, não técnica**: limiar de "Atenção" (7 dias) e o
próprio modelo de Meta como self-service do mentorado (achado no mockup, não no `spec.md`) ainda
precisam de validação com o cliente — ver Suposições na seção do Blueprint acima.

### M10 — E4 · Tarefas & Agenda

**Por que Médio:** "Tarefa" do E4 é o mesmo `Encaminhamento` que já existe desde o M06 (CLAUDE.md:
"Tarefas por encontro (encaminhamentos)... peso (1 ou 2) usado no ranking do E17") — não é uma
entidade nova, é evolução de schema numa entidade que já existe e já é consumida por dois módulos
prontos (E17/`ConsolidatedRepository`, M08/`MentoradoDashboardService`). O trabalho novo é dado
(status rico, prazo, prioridade, vínculo a meta) + self-service CRUD, não modelagem do zero — mesmo
porte do M09.

**H4.5 (Admin atribui peso) já está satisfeita desde o M06** via `AtaEncaminhamentoSugerido` +
`AtualizarSugestaoRequest` (`@Min(1) @Max(2)` em `pesoSugerido`, editável antes de publicar a ata).
Nenhum trabalho novo pra essa história nesta leva.

**Evolução de schema, risco avaliado como baixo:** troca do `boolean concluido` por um `status`
persistido (`PENDENTE`/`EM_ANDAMENTO`/`CONCLUIDA`) + `prazo` (nullable — encaminhamentos antigos e
os gerados por ata não têm prazo, só as tarefas self-service novas exigem) + `prioridade`
(`ALTA`/`MEDIA`/`BAIXA`, default `MEDIA`) + `meta_id` (FK nullable pra `Meta` do M09). Só 6 call
sites de `new Encaminhamento(...)` em todo o backend (`AtaService`, `DemoDataSeeder` ×2, mais 3 em
teste) — os construtores antigos (que recebem `boolean concluido`) **ficam exatamente como estão
por fora**, convertendo pra `status` por dentro; nenhum call site antigo muda. `isConcluido()` vira
método derivado (`status == CONCLUIDA`) em vez de getter de campo, então `MentoradoDashboardService`
(M08) e seus testes continuam compilando sem alteração. Único ponto que precisa mudar de verdade: a
JPQL de `ConsolidatedRepository.buscarConsolidado()` (E17), que referencia `e.concluido = true`
direto — vira `e.status = ...StatusTarefa.CONCLUIDA` — suíte completa do E17 roda de novo pra
confirmar zero regressão no ranking.

**Achado no mockup congelado (`design/prototipo/index.html`, seção TAREFAS):** botão "+ Nova
tarefa" — mesmo padrão self-service do M09, não coberto por H4.1–H4.5 (que são só leitura/filtro/
conclusão). **Decisão de integridade (achada agora, não deixada pro `revisor-seguranca` achar
depois):** peso alimenta o ranking ponderado do E17 (H17.2) — se o mentorado pudesse se
auto-atribuir peso 2 nas próprias tarefas, seria um vetor de gaming do próprio ranking. Tarefas
self-service nascem sempre com **peso 1 fixo, não editável pelo mentorado**; peso 2 continua
exclusivo do fluxo Admin/ata (H4.5). Vínculo a `Meta` (`metaId` no criar/editar) passa pela mesma
checagem de posse por tenant já usada no `MetaService` (meta tem que ser do mesmo mentorado, senão
404 genérico).

**Fora de escopo nesta leva, mesmo padrão "buraco esperado":** o calendário mensal do mockup
(widget decorativo) — H4.3 fala da lista refletir o filtro, calendário é visualização secundária
sem critério de aceite testável próprio. Paginação da lista também fica de fora (dataset pequeno
por mentorado, mesmo raciocínio do M08/M09).

**Sub-status "Atrasada" (H4.4):** mesmo padrão do M09 — não persistido, calculado no response
(`status != CONCLUIDA && prazo != null && prazo < hoje`), sobrepõe visualmente `PENDENTE`/
`EM_ANDAMENTO` sem criar um 4º valor de enum persistido.

## Modelagem de banco (M10)

```sql
-- V7__tarefas.sql
ALTER TABLE encaminhamento ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';
UPDATE encaminhamento SET status = 'CONCLUIDA' WHERE concluido = true;
ALTER TABLE encaminhamento ADD CONSTRAINT chk_encaminhamento_status
    CHECK (status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDA'));
ALTER TABLE encaminhamento DROP COLUMN concluido;

ALTER TABLE encaminhamento ADD COLUMN prazo DATE;
ALTER TABLE encaminhamento ADD COLUMN prioridade VARCHAR(10) NOT NULL DEFAULT 'MEDIA';
ALTER TABLE encaminhamento ADD CONSTRAINT chk_encaminhamento_prioridade
    CHECK (prioridade IN ('ALTA','MEDIA','BAIXA'));
ALTER TABLE encaminhamento ADD COLUMN meta_id UUID REFERENCES meta(id);

CREATE INDEX idx_encaminhamento_status ON encaminhamento(status);
```

## Contratos de API (M10)

```jsonc
POST /api/v1/mentorado/tarefas            // hasRole("MENTORADO"); peso sempre 1, não vem do request
{ "titulo": "Revisar indicadores da semana", "prazo": "2026-07-18", "prioridade": "ALTA", "metaId": "uuid|null" }

GET /api/v1/mentorado/tarefas?status=&busca=   // status e busca opcionais
[{
  "id": "uuid", "titulo": "...", "metaRelacionada": { "id": "uuid", "titulo": "..." } | null,
  "prazo": "2026-07-18", "diasRestantes": 3, "prioridade": "ALTA",
  "status": "PENDENTE", "atrasada": false, "peso": 1
}]

GET /api/v1/mentorado/tarefas/resumo       // H4.2/H4.3 — sempre sobre TODAS, ignora o filtro da tela
{ "total": 24, "concluidas": 14, "emAndamento": 7, "pendentes": 3 }

PUT /api/v1/mentorado/tarefas/{id}         // título/prazo/prioridade/metaId — peso não é editável
{ "titulo": "...", "prazo": "2026-07-20", "prioridade": "MEDIA", "metaId": "uuid|null" }

PATCH /api/v1/mentorado/tarefas/{id}/status // { "novoStatus": "EM_ANDAMENTO" | "CONCLUIDA" | "PENDENTE" }
```

## Rastreabilidade história ↔ módulo (M10)

| História | Cobertura |
|---|---|
| H4.1 — tabela tarefa/meta/prazo/status/prioridade | `GET /mentorado/tarefas` |
| H4.2 — marcar concluída | `PATCH /mentorado/tarefas/{id}/status` |
| H4.3 — filtrar + buscar | `GET /mentorado/tarefas?status=&busca=` + `GET /tarefas/resumo` |
| H4.4 — sinalizar Atrasada | `GET /mentorado/tarefas` (`atrasada`, calculado) |
| H4.5 — Admin atribui peso | já satisfeita desde o M06 (`AtualizarSugestaoRequest`) |

**Status: ✅ M10 concluído** (2026-07-09) — backend (178/178 testes, incluindo `TarefaServiceTest` novo
e `ConsolidatedServiceTest`/`MentoradoConsolidadoResponseTest` reconfirmados pós-migração de
`e.concluido` → `e.status`), evolução de schema em entidade compartilhada (`Encaminhamento`, migração
`V7__tarefas.sql`) sem regressão em M06/E17/M08. `revisor-seguranca` sem achado bloqueante — **6
pontos escrutinados, nenhuma falha real**: (1) peso do ranking protegido: `CriarTarefaRequest`/
`AtualizarTarefaRequest` sem campo `peso`, construtor self-service fixa `peso = 1` hardcoded, único
caminho de `peso = 2` permanece exclusivo do fluxo Admin/ata (`@RequiresModulo(Modulo.MENTORADOS)`,
rota `/api/v1/admin/**`); (2) isolamento `Tarefa→Meta` confirmado em `criar()` e `atualizar()` sem
exceção (`resolverMetaDoUsuario()` filtra por `mentorado.getId()`, 404 genérico se meta de outro
mentorado); (3) isolamento da própria tarefa: `buscarDoMentorado()` bloqueia edição/transição de
tarefa alheia, mesmo padrão 404 genérico; (4) migração idempotente — `concluido` era
`NOT NULL DEFAULT FALSE` desde V1, sem NULL ambíguo, DDL transacional Postgres, sem janela de
inconsistência; (5) máquina de estado com guardas explícitas e `switch` sem `default` silencioso;
(6) JPQL `buscarPorMentorado` usa bind parameters (`@Param`) incluindo dentro de
`CAST(:busca AS string)` e `LIKE/CONCAT` — sem risco de injeção.

**3 bugs reais corrigidos durante verificação ao vivo** (nenhum pego pelos testes com mock — mesma
classe de achado do M05, ver `LeadRepositoryTest`): (1) `LazyInitializationException` em `Meta` ao
ler `tarefa.getMeta().getTitulo()` a partir do RETORNO de `encaminhamentoRepository.save(tarefa)` —
`save()` numa entidade já persistida faz `merge()`, que devolve um objeto gerenciado num contexto de
persistência NOVO, onde a associação volta a proxy LAZY não inicializado mesmo com FETCH JOIN na
busca original; corrigido em `TarefaService.atualizar()`/`avancarStatus()` retornando a referência
pré-save (já com `meta` carregada), não o valor de `save()` — regressão travada em
`EncaminhamentoRepositoryTest` (`@DataJpaTest`, RED+GREEN); (2) `Invalid Date` visível no frontend
quando `prazo` é `null` (comum em encaminhamentos antigos/gerados por ata) — `formatarPrazo()` em
`TarefasPage.tsx` não tinha guard pra esse caso, corrigido pra mostrar "Sem prazo"; (3) race condition
de fetch fora de ordem em `TarefasPage.tsx` — cliques rápidos em sequência (ex.: Concluir logo após
trocar filtro) disparavam `carregar()` mais de uma vez em paralelo, e a resposta mais antiga podia
sobrescrever a mais nova; corrigido com um `requestIdRef` que só aplica a resposta do fetch mais
recente (mesmo risco estrutural existe em `MetasPage.tsx`/M09, não retrabalhado nesta leva — ver
memória de metodologia). Também achado e corrigido um bug no próprio E2E: uma asserção assumia que
concluir uma tarefa a removeria da view, mas o filtro ativo no momento era "Todas" (mostra todo
status) — a asserção só "passava" por coincidência ao pegar a janela de `tarefas === null`
(estado de carregamento); corrigida pra checar a mudança do pill de status em vez da ausência da
linha.

Frontend (`TarefasPage` + rota + nav no `MentoradoShell`) e E2E (`tarefas.spec.ts`, 3 testes) —
**35/35 verde na suíte completa**, confirmado com `--repeat-each=10` no teste de ciclo de vida após
a correção acima. Sem pendência de credencial externa.

### M11 — E6 · Materiais & Dicas do Brayan

**Por que Médio:** Módulo do lado do mentorado, focado em leitura, curadoria (favoritos) e consumo (assistido) de entidades `Conteudo` já criadas e gerenciadas pelo Admin desde o M06. A complexidade fica na junção de dados globais (o catálogo) com estado local por tenant (o que o mentorado atual favoritou/consumiu).

**Decisões de escopo & Suposições assumidas:**
- **Categorias (H6.1):** O `spec.md` cita filtro por "categoria e formato", mas `Conteudo` tem apenas `tipo` (formato: VIDEO, DOCUMENTO, etc.), sem coluna de categoria temática (Vendas, Gestão etc.) — filtro implementado só por `tipo`. Se a SAW pedir categorização temática, entra como coluna nova depois.
- **Controle de acesso por plano reaproveita o padrão do M08:** `planosPermitidos()` compara `Plano.ordinal()` (mesma técnica, e o mesmo aviso, do `dicaDestaque` do M08 — funciona porque a ordem declarada do enum já é a hierarquia de negócio, mas é frágil a reordenar `Plano.java`).
- **Indicadores de consumo (H6.3) — pendência real, não só suposição:** a história pede "o consumo conta nos meus indicadores (dias assistidos, minutos, favoritas)". O que existe hoje é só o toggle `assistido`/`favorito` por item (`ConteudoMentorado.dataConsumo` grava a data na 1ª vez que é marcado assistido) — **não existe nenhum endpoint ou tela agregando esses dados num "indicador"** (nem contagem de dias assistidos, nem minutos, nem um resumo de favoritas). Diferente das Suposições acima (decisões conscientes), isto é um pedaço da história não implementado — ver Status abaixo.

## Modelagem de banco (M11)

```sql
-- V8__conteudo_mentorado.sql
CREATE TABLE conteudo_mentorado (
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id) ON DELETE CASCADE,
    conteudo_id     UUID NOT NULL REFERENCES conteudo(id) ON DELETE CASCADE,
    favorito        BOOLEAN NOT NULL DEFAULT false,
    assistido       BOOLEAN NOT NULL DEFAULT false,
    data_consumo    TIMESTAMP,
    PRIMARY KEY (mentorado_id, conteudo_id)
);

CREATE INDEX idx_conteudo_mentorado_favorito ON conteudo_mentorado(mentorado_id, favorito);
CREATE INDEX idx_conteudo_mentorado_assistido ON conteudo_mentorado(mentorado_id, assistido);

-- V9__conteudo_mentorado_versao.sql — achado nesta verificação: a entidade nasceu sem @Version
-- (não estende BaseEntity porque usa @EmbeddedId composta, incompatível com o @Id de coluna
-- única de BaseEntity). Toda entidade do projeto tem lock otimista desde o achado da revisão de
-- segurança do E14 — replicado aqui manualmente numa migração separada, já que V8 já tinha sido
-- aplicada neste ambiente antes desta verificação (editar V8 direto quebraria o checksum do Flyway).
ALTER TABLE conteudo_mentorado ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
```

## Contratos de API (M11)

```jsonc
// hasRole("MENTORADO") — traz catálogo filtrado por planoMinimo <= plano do mentorado
GET /api/v1/mentorado/conteudos?tipo=&favorito=
[{
  "id": "uuid", "titulo": "Ficha Técnica Completa", "tipo": "PLANILHA", "url": "https://...",
  "planoMinimo": "BASICO", "publicado": true, "criadoEm": "2026-06-01T12:00:00Z",
  "favorito": true, "assistido": false
}]

// Mesmo formato acima, só tipo=VIDEO, ordenado pelos mais recentes (proxy pra "Dicas do Brayan")
GET /api/v1/mentorado/conteudos/dicas

PATCH /api/v1/mentorado/conteudos/{id}/favorito
{ "favorito": true }

PATCH /api/v1/mentorado/conteudos/{id}/assistido   // 1ª vez marcando true grava data_consumo
{ "assistido": true }
// 404 se o conteúdo não existe; 403 se o plano do mentorado não permite (AccessDeniedException,
// não IllegalStateException — achado nesta verificação, corrigido pra bater com o padrão já
// usado em GlobalExceptionHandler pro resto do projeto)
```

## Rastreabilidade história ↔ módulo (M11)

| História | Cobertura |
|---|---|
| H6.1 — navegar biblioteca por categoria e formato | `GET /mentorado/conteudos` (só `tipo`, sem categoria temática — ver Suposição acima) |
| H6.2 — favoritar materiais e dicas | `PATCH /mentorado/conteudos/{id}/favorito` |
| H6.3 — assistir dicas | `GET /mentorado/conteudos/dicas`, `PATCH /mentorado/conteudos/{id}/assistido` — cobre "assistir", **não cobre** "contar nos meus indicadores" (nenhum endpoint agrega dias/minutos/favoritas ainda, ver Pendência acima) |

**Status: ✅ M11 concluído** (2026-07-09) — módulo começou como código escrito em paralelo por
outra sessão (não seguiu o pipeline Blueprint→TDD→revisor-seguranca→E2E desta esteira), adotado e
completado por esta sessão antes de fechar. Backend (183/183 testes) + frontend (`MateriaisPage`,
rota `/mentorado/materiais` + nav no `MentoradoShell`) + E2E (`materiais.spec.ts`, 3 testes) —
**38/38 verde na suíte completa**.

**Achados corrigidos na verificação desta sessão, antes do `revisor-seguranca`:**
CSS com variáveis inexistentes no design system (`--text-base`/`--bg-hover`/`--primary`/
`--bg-base` em vez de `--text`/`--elevated`/`--gold`/`--surface` — a tela renderizaria com texto
provavelmente invisível no tema dark) + baixo contraste (texto branco sobre fundo dourado no botão
de acessar material, corrigido pra `--on-gold`); rota `/mentorado/materiais` nunca tinha sido
adicionada em `App.tsx` (nav linkava pra uma página inacessível); `ConteudoMentoradoService`
lançava `IllegalStateException` (409) tanto pra "não encontrado" quanto "plano insuficiente" —
trocado pra `NoSuchElementException` (404) e `AccessDeniedException` (403), consistente com o
padrão do resto do projeto; **achado de isolamento real**: `atualizarStatus` não checava
`conteudo.isPublicado()` — um mentorado que soubesse/adivinhasse o UUID de um conteúdo ainda em
rascunho conseguia favoritar/marcar assistido nele, vazando título/url de volta na resposta;
corrigido tratando como 404; `ConteudoMentorado` não estendia `BaseEntity` (não tinha `@Version`,
diferente de toda outra entidade do projeto desde o achado do E14) — corrigido com `@Version`
manual + migração `V9__conteudo_mentorado_versao.sql` nova, já que `V8` já tinha sido aplicada
neste ambiente antes desta verificação (editá-la quebraria o checksum do Flyway — acabou
acontecendo mesmo assim uma vez, revertido); parâmetro `mentoradoId` renomeado pra `usuarioId`
(era na verdade um id de Usuario resolvido internamente, nome antigo convidava um call site futuro
a pular a resolução a partir do usuário autenticado).

**Achados do `revisor-seguranca` (4, todos corrigidos):** (1) `window.open(dica.url, '_blank')`
sem `noopener,noreferrer` — reverse tabnabbing, inconsistente com os `<a rel="noreferrer">` já
usados na mesma página; (2) `ConteudoMentoradoService.planosPermitidos()` duplicava a comparação
por `Plano.ordinal()` já usada em `MentoradoDashboardService` (M08), invalidando o comentário que
dizia "único ponto do backend" — centralizado em `Plano.atendePlanoMinimo()`, comentário do M08
corrigido; (3) lock otimista (`@Version`) só protege UPDATE-vs-UPDATE — duas requisições
concorrentes do *primeiro* favoritar/assistir (mesmo mentorado, ex. duplo clique) podiam colidir
na PK composta e estourar 500 em vez de 409; adicionado `GlobalExceptionHandler.
handleDataIntegrityViolation`; (4) `Conteudo.url` (CRUD Admin, M06) sem validação de esquema —
M11 é a primeira vez que esse campo vira link clicável/`window.open` direto pro mentorado, então
um Admin comprometido podia gravar `javascript:...`; adicionado `@Pattern(regexp = "^https?://.+")`
em `CriarConteudoRequest`/`AtualizarConteudoRequest`.

**Pendência real (não suposição):** H6.3 pede "o consumo conta nos meus indicadores (dias
assistidos, minutos, favoritas)" — não implementado, só o toggle por item existe. Fica pra uma
leva futura quando/se a SAW confirmar que precisa desse indicador agregado (ver Suposições do
Blueprint acima). Sem pendência de credencial externa.

### M12 — E5 · Mentorias & Atas (lado mentorado)

**Por que Médio:** o dado já existe desde o M06 (`Mentoria`, `Ata`, `AtaEncaminhamentoSugerido`,
todos admin-side) — o trabalho real é (a) uma tela de leitura pro mentorado por cima dessas mesmas
entidades, com isolamento por tenant e filtragem de status que o lado Admin não precisa (rascunho de
ata nunca visível), (b) uma janela de tempo computada ("posso entrar agora?") que não existe em
lugar nenhum ainda, e (c) fechar um buraco de schema real descoberto na investigação: a tabela
`mentoria_material_recomendado` existe desde o `V5__mentorias.sql` (M06) mas nunca foi mapeada em
JPA nem exposta em endpoint algum, nem Admin nem mentorado — sem isso, H5.2 nunca teria o que
mostrar no campo "materiais recomendados".

**Decisões de escopo & Suposições assumidas:**
- **Janela de "posso entrar agora" (H5.1):** `spec.md` só diz "quando chega o horário" sem definir
  quanto antes o botão libera. Assumido: 10 minutos antes de `dataHora` até `dataHora + duracaoMin`
  (fim previsto), exigindo `linkOnline` preenchido e `status` em `AGENDADA`/`CONFIRMADA` (uma
  mentoria `CANCELADA` ou já `REALIZADA` nunca mostra o botão). Ajustável se a SAW pedir outro valor
  — documentado aqui pra não virar "mágica" no código.
- **`PATCH /admin/mentorias/{id}/materiais` é escopo novo do lado Admin, mas dentro deste módulo, não
  fora dele:** é pré-requisito direto de H5.2 — sem uma forma de o Admin associar `Conteudo`s a uma
  `Mentoria`, o array `materiaisRecomendados` seria sempre vazio e a história ficaria inverificável
  por E2E. Pequena extensão do `MentoriaController` já existente (mesmo `@RequiresModulo`), não um
  módulo novo.
- **Materiais recomendados respeitam o mesmo `Plano.atendePlanoMinimo()` do M11**, mesmo sendo uma
  recomendação direta do mentor: decisão consciente pra não abrir um segundo caminho que contorne o
  paywall por plano (`CLAUDE.md` § Planos diz que planos "controlam acesso a conteúdos" sem ressalva).
  Também exige `conteudo.isPublicado()` — mesmo invariante do M11, mesmo motivo (não vazar rascunho).
- **Sem rota de detalhe separada.** `spec.md` descreve um fluxo "vejo a lista, abro uma, vejo a ata" —
  mas com a escala do projeto (10–15 usuários, poucas mentorias cada, ver `CLAUDE.md` § Princípios ·
  Escala) não compensa uma segunda chamada de API só pra abrir um item: `GET /mentorado/mentorias`
  já devolve tudo (ata resumida + materiais) por item, e o frontend expande a linha/card já carregado.
  Mesmo padrão de "lista completa, sem paginação" já usado em Metas/Tarefas.
- **Ata exposta ao mentorado é um subconjunto deliberado de `AtaResponse` (admin):** só `resumo` e
  `publicadaEm`. Nunca `transcricao`, `erroProcessamento` nem `sugestoes` — são dados internos do
  pipeline de IA / pré-revisão humana, sem valor nem permissão pro mentorado ver. E só aparece quando
  `status == PUBLICADA`; `AtaService.buscarPorMentoria`/`AtaRepository.findByMentoriaId` não filtram
  por status hoje (correto pro Admin, que precisa ver rascunhos) — este é o primeiro caminho de
  leitura de Ata pelo lado do mentorado, então o filtro de `PUBLICADA` nasce aqui, não é alteração
  em código existente.
- **Encaminhamentos gerados por uma ata publicada não precisam de trabalho novo aqui:**
  `AtaService.publicar()` já materializa sugestões aceitas em `Encaminhamento` reais (um por
  mentorado participante), e esses já aparecem na tela de Tarefas existente desde o M10
  (`Encaminhamento.mentoria` como referência de origem). O laço já fecha ponta a ponta — este módulo
  só cobre a ata em si (resumo) e a agenda/histórico da mentoria, não duplica a lista de tarefas.
- **H5.3 (calendário) cobre as duas opções do "(.ics/Google)" da BDD sem duplicar lógica:** endpoint
  dedicado gera e devolve um `.ics` pra download direto; o link "Adicionar ao Google Calendar" é
  construído 100% no frontend a partir dos mesmos campos já presentes na resposta da lista (URL de
  render do Google Calendar aceita todos os parâmetros por query string) — sem round-trip extra ao
  backend só pra isso.

## Modelagem de banco (M12)

```sql
-- Nenhuma migração nova. mentoria_material_recomendado já existe desde V5__mentorias.sql (M06):
--   CREATE TABLE mentoria_material_recomendado (
--       mentoria_id  UUID NOT NULL REFERENCES mentoria(id) ON DELETE CASCADE,
--       conteudo_id  UUID NOT NULL REFERENCES conteudo(id),
--       PRIMARY KEY (mentoria_id, conteudo_id)
--   );
-- — só nunca tinha sido mapeada em JPA. Este módulo adiciona o @ManyToMany em Mentoria.java
-- apontando pra esta tabela existente, sem alterar schema.
```

## Contratos de API (M12)

```jsonc
// hasRole("MENTORADO") — todas as mentorias do mentorado autenticado (agenda + histórico juntos,
// ordenado por dataHora ASC; front agrupa em "Agenda" vs "Histórico" e reordena cada grupo)
GET /api/v1/mentorado/mentorias
[{
  "id": "uuid", "tipo": "INDIVIDUAL", "mentorNome": "Brayan Silva",
  "dataHora": "2026-07-15T14:00:00Z", "duracaoMin": 60,
  "linkOnline": "https://meet.google.com/abc-defg-hij", "local": null,
  "status": "CONFIRMADA", "podeEntrarAgora": false,
  "ata": null,
  "materiaisRecomendados": []
}, {
  "id": "uuid2", "tipo": "INDIVIDUAL", "mentorNome": "Brayan Silva",
  "dataHora": "2026-06-10T14:00:00Z", "duracaoMin": 60,
  "linkOnline": "https://meet.google.com/xyz", "local": null,
  "status": "REALIZADA", "podeEntrarAgora": false,
  "ata": { "resumo": "Discutimos o DRE de junho e ajustamos metas.", "publicadaEm": "2026-06-10T15:30:00Z" },
  "materiaisRecomendados": [{ "id": "uuid3", "titulo": "Ficha Técnica Completa", "tipo": "PLANILHA", "url": "https://..." }]
}]

// 404 se a mentoria não existe OU o mentorado autenticado não é participante — mesmo padrão de
// oráculo de enumeração já usado no resto do projeto (não distingue "não existe" de "não é seu").
GET /api/v1/mentorado/mentorias/{id}/calendario.ics
// Content-Type: text/calendar; charset=utf-8 · Content-Disposition: attachment; filename="mentoria.ics"

// Novo — extensão pequena do Admin, pré-requisito de H5.2 (ver Suposições acima).
PATCH /api/v1/admin/mentorias/{id}/materiais
{ "conteudoIds": ["uuid3", "uuid4"] }
// Substitui a lista inteira (idempotente, não incremental). 400 (IllegalArgumentException) se a
// mentoria ou algum conteudoId não existir — mesma convenção dos métodos irmãos de MentoriaService
// (criar/buscar), não o 404 do lado mentee-facing: convenções diferentes por design, admin-only
// não tem o mesmo risco de oráculo de enumeração que justificou o 404 nas rotas do mentorado.
// Mesmo @RequiresModulo(Modulo.MENTORADOS) do resto de MentoriaController.
```

## Rastreabilidade história ↔ módulo (M12)

| História | Cobertura |
|---|---|
| H5.1 — ver próxima mentoria e entrar na reunião | `GET /mentorado/mentorias` (campo `podeEntrarAgora`, ver Suposições) |
| H5.2 — histórico e ata de cada mentoria | `GET /mentorado/mentorias` (`ata`, `materiaisRecomendados`), `PATCH /admin/mentorias/{id}/materiais` (curadoria, novo) |
| H5.3 — adicionar ao calendário | `GET /mentorado/mentorias/{id}/calendario.ics` + link Google Calendar (frontend) |

**Status: ✅ M12 concluído** (2026-07-09) — backend (213/213 testes, incluindo
`MentoriaMentoradoResponseTest` novo para a janela de "posso entrar agora", `IcsGeneratorTest` novo,
`MentoriaMentoradoServiceTest` novo, `MentoriaRepositoryTest` novo) + frontend (`MentoriasPage`, rota
`/mentorado/mentorias` + nav no `MentoradoShell`, reposicionada antes de "Materiais & Dicas" pra bater
com a ordem pretendida do CLAUDE.md) + E2E (`mentorias.spec.ts`, 4 testes) — **42/42 verde na suíte
completa**.

**Bug real achado na verificação ao vivo via curl, antes do `revisor-seguranca`:** ao rodar a suíte
completa de E2E depois de fechar o módulo, `mentorados.spec.ts` (M06) quebrou — a listagem de
mentorias do Admin (`GET /admin/mentorias`, que alimenta a tela de criar/confirmar mentoria)
começou a estourar 500. Causa: `MentoriaResponse.from()` passou a ler
`m.getMateriaisRecomendados()` (novo campo, H5.2), mas `MentoriaRepository.buscarPorStatus`/
`buscarPorIdComDetalhes` (usadas pelo Admin) nunca faziam `LEFT JOIN FETCH` nessa coleção —
`LazyInitializationException` fora da transação (open-in-view=false), mesma classe de bug do M05/
M10 (ver `EncaminhamentoRepositoryTest`). `buscarPorMentorado` (mentee-facing) já tinha o fetch join
certo, mas as duas queries do lado Admin não. Corrigido adicionando o mesmo `LEFT JOIN FETCH` às
duas, com `MentoriaRepositoryTest` novo (`@DataJpaTest`, sessão real do Hibernate) provando que a
coleção fica legível mesmo depois de `entityManager.clear()`.

**Achados do `revisor-seguranca` (2, ambos corrigidos):** (1) **Medium** — `IcsGenerator.escapar()`
tratava `\`, `,`, `;` e `\n`, mas nunca `\r` isolado; como o próprio gerador usa `\r\n` como
terminador de linha, um `\r` cru dentro de um campo livre (`local` da mentoria, nome do mentor)
virava um terminador de linha extra pra qualquer parser de calendário tolerante a CR solto,
permitindo injetar propriedades/componentes forjados (ex.: um `VALARM` falso) dentro do `.ics` que
o mentorado baixa confiando vir do SAW HUB — corrigido escapando `\r` (e `\r\n`) pro mesmo `\\n` do
LF, com 2 testes de regressão novos provando que a string injetada vira texto inofensivo dentro do
campo, nunca uma linha própria; (2) **Baixo/informativo** — `Mentoria.linkOnline` nunca teve
validação de esquema de URL (diferente de `Conteudo.url`, corrigido no M11), e o M12 é a primeira
vez que esse campo vira alvo recorrente de botão "Entrar na reunião"/.ics/Google Calendar pro
mentorado (antes só um link discreto no dashboard do M08) — corrigido com o mesmo
`@Pattern(regexp = "^https?://.+")` já usado em `Conteudo.url`, campo continua opcional (mentoria
presencial não tem link).

**Pendência real, documentada, não escondida:** o endpoint Admin `PATCH /admin/mentorias/{id}/materiais`
existe e funciona (verificado via curl e testes), mas não há controle nenhum na tela do Admin pra
usá-lo — associar materiais recomendados a uma mentoria hoje só é possível via API direta. Fica pra
uma leva futura, mesma categoria da pendência do H6.3 (M11): o dado/endpoint existe, falta só a UI
de curadoria. Sem pendência de credencial externa.

### M13 — E7 · Eventos & Inscrições (lado mentorado)

**Por que Médio (na borda de Grande):** diferente de M12 (só leitura sobre dado que já existia),
aqui H7.2 ("Inscrever-se") não tem NENHUM precedente no código — `Evento` (M06) é só CRUD admin,
sem tabela de inscrição, sem contagem de ocupação. Este módulo cria uma entidade nova com máquina
de estado própria (`InscricaoEvento`), uma migração nova, e uma corrida de concorrência real
(duas inscrições simultâneas na última vaga) que precisa do mesmo tratamento de lock otimista já
padronizado desde o E14 — não é só "mais uma tela de leitura" como M12.

**Decisões de escopo & Suposições assumidas:**
- **`InscricaoEvento` é uma entidade nova, com `@EmbeddedId(mentoradoId, eventoId)`** — mesmo
  padrão de `ConteudoMentorado` (M11): unicidade natural (não faz sentido duas inscrições do mesmo
  mentorado no mesmo evento), sem `BaseEntity` (incompatível com `@EmbeddedId`), `@Version` manual
  replicado (mesmo motivo do M11: proteger contra corrida). Estados:
  `StatusInscricao { INSCRITA, CANCELADA, PARTICIPOU }` — bate exatamente com a máquina do
  `CLAUDE.md` § Máquinas de estado ("Disponível → Inscrito → Participado · desvio: → Cancelada"),
  onde "Disponível" nunca é persistido (é só "a linha ainda não existe"). Cancelar e reinscrever
  reaproveitam a MESMA linha (a chave composta impede duplicata) — cancelar não é permanente.
- **`PARTICIPOU` é automático, não manual:** não existe (nem entra nesta leva) uma tela de check-in
  de presença no Admin — construir isso seria escopo novo do lado Admin bem além de H7. Em vez
  disso, `EventoService.finalizar()` (existente, `AO_VIVO → REALIZADO`) ganha um hook: toda
  `InscricaoEvento` ainda `INSCRITA` daquele evento vira `PARTICIPOU` na mesma transição. Simples,
  automático, consistente — se o evento aconteceu e o mentorado não cancelou, participou.
- **Corrida na última vaga — mesmo padrão de lock otimista do E14/M11, não uma trava nova:**
  `Evento` ganha um campo novo `vagasOcupadas` (contador, não `COUNT(*)` ao vivo) mutado só dentro
  de `inscrever()`/`cancelar()`, na MESMA transação que salva a `InscricaoEvento` — como `Evento` já
  tem `@Version` (`BaseEntity`), duas inscrições concorrentes na última vaga fazem a segunda `save()`
  do `Evento` estourar `ObjectOptimisticLockingFailureException` (409, já mapeado em
  `GlobalExceptionHandler` desde o E14), não um estouro silencioso de vaga. Um `COUNT(*)` ao vivo
  não teria essa proteção de graça — só um campo mutado dentro do `@Version` já existente tem.
- **Sem endpoint de calendário separado (H7.3):** mesma decisão do M12 (sem rota de detalhe) — a
  escala é pequena (poucos eventos, ver seed), `GET /mentorado/eventos` já traz `dataHora` de cada
  um, e o frontend agrupa por dia num componente de calendário sem round-trip extra.
  "Próximos eventos" (H7.2, pós-inscrição) também não é endpoint separado: a mesma lista já traz um
  campo `inscrito: boolean` por evento, e o frontend separa/realça client-side — mesmo padrão de
  agenda/histórico do M12.
- **Escopo de leitura limitado a `PROGRAMADO`/`AO_VIVO`:** `spec.md` H7.1 diz "Dado eventos
  **programados**" — diferente de M12 (que tem histórico explícito em H5.2), H7 não pede uma visão
  de eventos passados. `GET /mentorado/eventos` só retorna esses dois status; `REALIZADO`/
  `CANCELADO` ficam de fora por decisão consciente, não esquecimento.
- **`linkOnline` ganha `@Pattern(regexp = "^https?://.+")` proativamente**, antes mesmo do
  `revisor-seguranca` apontar — mesma lição do M12 (achado 2): `Evento.linkOnline` nunca foi
  validado, e este módulo é a primeira vez que ele vira link clicável pro mentorado (Admin só usava
  internamente até aqui). Aplicar o padrão já conhecido de cara evita reabrir o mesmo achado pela
  terceira vez.

## Modelagem de banco (M13)

```sql
-- V10__inscricao_evento.sql
ALTER TABLE evento ADD COLUMN vagas_ocupadas INT NOT NULL DEFAULT 0;

CREATE TABLE inscricao_evento (
    mentorado_id    UUID NOT NULL REFERENCES mentorado(id) ON DELETE CASCADE,
    evento_id       UUID NOT NULL REFERENCES evento(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'INSCRITA',
    versao          BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (mentorado_id, evento_id),
    CONSTRAINT chk_inscricao_status CHECK (status IN ('INSCRITA','CANCELADA','PARTICIPOU'))
);
-- Sem criado_em de propósito, mesmo padrão do ConteudoMentorado (M11): não estende BaseEntity
-- (@EmbeddedId incompatível), e nenhuma história pede "desde quando" — não adicionar campo sem
-- uso concreto.

-- evento nunca ganhou esses dois índices no V5 (M06) — aproveitado aqui já que este módulo é o
-- primeiro a filtrar/ordenar por eles em volume (agenda + calendário do mentorado).
CREATE INDEX idx_evento_status ON evento(status);
CREATE INDEX idx_evento_data_hora ON evento(data_hora);
CREATE INDEX idx_inscricao_evento_mentorado ON inscricao_evento(mentorado_id);
```

## Contratos de API (M13)

```jsonc
// hasRole("MENTORADO") — só PROGRAMADO/AO_VIVO (ver Suposições acima)
GET /api/v1/mentorado/eventos?tipo=&tema=
[{
  "id": "uuid", "titulo": "Encontro Nacional SAW 2026", "tipo": "AO_VIVO", "tema": "Gestão de restaurantes",
  "dataHora": "2026-09-10T19:00:00Z", "local": null, "linkOnline": "https://meet.google.com/encontro-saw",
  "vagas": 200, "vagasDisponiveis": 198, "status": "PROGRAMADO", "inscrito": true
}]
// vagasDisponiveis é null se vagas for null (evento sem limite de capacidade)

POST /api/v1/mentorado/eventos/{id}/inscricao
// 201 -> mesma forma acima, "inscrito": true
// 409 (IllegalStateException) se vagasDisponiveis <= 0, ou se o evento não está PROGRAMADO/AO_VIVO
// 404 se o evento não existe

DELETE /api/v1/mentorado/eventos/{id}/inscricao
// 204 — cancela a própria inscrição (nunca a de outro mentorado: sempre resolvida por
// mentorado+evento, nunca por um id de InscricaoEvento vindo de fora)
// 404 se não existe inscrição ativa (INSCRITA) deste mentorado nesse evento
```

## Rastreabilidade história ↔ módulo (M13)

| História | Cobertura |
|---|---|
| H7.1 — ver eventos ao vivo/presenciais, filtrar tipo/tema, ver data/local/participantes | `GET /mentorado/eventos?tipo=&tema=` (`vagasDisponiveis` cobre "participantes") |
| H7.2 — inscrever-se num evento com vagas | `POST /mentorado/eventos/{id}/inscricao`, campo `inscrito` reflete "Próximos eventos" |
| H7.3 — calendário de eventos por dia | `GET /mentorado/eventos` (frontend agrupa por `dataHora`, sem rota nova) |

**Status: ✅ M13 concluído** (2026-07-09) — backend (226/226 testes, incluindo `InscricaoEvento`/
`StatusInscricao` novos, `EventoMentoradoServiceTest` com 12 testes cobrindo listar/inscrever/
cancelar/corrida de vaga/reinscrição, `EventoServiceTest` com o hook de participação novo) +
frontend (`EventosMentoradoPage`, rota `/mentorado/eventos` + nav no `MentoradoShell`, calendário
mensal construído do zero — sem precedente nem lib externa) + E2E (`eventos.spec.ts`, 4 testes) —
**46/46 verde na suíte completa**.

**Achado ao vivo, antes do `revisor-seguranca`:** mesma classe de drift do M12 (lição 4) — o
evento seedado "Workshop de Gestão Financeira" estava `REALIZADO` no banco de dev, não
`PROGRAMADO` como o `DemoDataSeeder` atual sempre gera, resquício de uma verificação ao vivo bem
anterior nesta sessão (M06). Corrigido resetando a linha pra bater com o seeder — não era bug do
código novo, o filtro `PROGRAMADO`/`AO_VIVO` estava correto desde o início.

**`revisor-seguranca`: sem achado bloqueante** — primeira revisão totalmente limpa desde M08/M09/
M10 (a proatividade de aplicar `@Pattern` em `linkOnline` antes mesmo da revisão, lição do M12,
compensou). Duas notas informativas, nenhuma bloqueante: (1) validação de `linkOnline` não cobre
retroativamente eventos hipotéticos criados antes desta migração num banco de produção futuro —
não é falha deste código, é cuidado de dado ao implantar; (2) janela de corrida genuína, baixo
impacto: se uma inscrição for criada exatamente durante a transação que marca um evento como
`REALIZADO`, essa inscrição pode ficar presa em `INSCRITA` sem nunca virar `PARTICIPOU` (não vaza
dado nem quebra RBAC, só deixa um registro histórico raro e levemente inconsistente).

**Pendências reais, documentadas, não escondidas:** (1) a janela de corrida `inscrever()` vs.
`marcarParticipacoes()` acima — aceitável nesta escala (10-15 usuários), sem tela de check-in
manual no Admin nesta leva, revisitar se o volume crescer; (2) sem UI de edição no Admin pra
`Evento` (endpoint `PUT /admin/eventos/{id}` já existia desde o M06, mas o frontend Admin nunca
ligou um formulário de edição a ele) — achado ao ler `EventosPage.tsx` (Admin) durante o Blueprint,
não é escopo deste módulo mentee-facing, só registrado pra não se perder. Sem pendência de
credencial externa.

### M14 — E8 · Loja SAW (catálogo, carrinho, checkout, gateway)

**Por que Grande · risco alto:** diferente de E5/E7, não há NENHUM precedente admin (M06 nunca
tocou Loja) — `Produto`, `Pedido`, `ItemPedido` são inteiramente novos, junto com a primeira
integração de gateway de pagamento externo do projeto. Risco alto por decisão explícita do
CLAUDE.md ("Auth e Pagamento são os módulos de maior risco... passam por revisor-seguranca
obrigatório") — mesmo tratamento do E1.

**Decisão de gateway (pergunta feita a Marcos, resolvida antes deste Blueprint):
Mercado Pago**, via Checkout Pro (Preferences API) — o mentorado é redirecionado pro checkout
hospedado pelo Mercado Pago, o SAW HUB nunca processa dado de cartão diretamente (reduz escopo de
PCI a zero). Confirma o que já era esperado pela CLAUDE.md (público 100% Brasil, PIX/boleto/cartão).

**Decisões de escopo & Suposições assumidas:**
- **Chamada HTTP direta via `RestClient`, não o SDK oficial do Mercado Pago.** A superfície
  necessária é pequena (criar Preference, consultar um Payment por id) — usar o mesmo padrão já
  estabelecido em `com.sawhub.hub.mentoria.ia` (`WhisperTranscricaoService`/
  `ClaudeAtaRascunhoService`: `RestClient.Builder` autoconfigurado, timeout explícito, exceção
  sentinela própria, falha limpa sem credencial) evita uma dependência nova só pra duas chamadas,
  e mantém controle total pra testar sem credencial real.
- **Sem credencial neste ambiente — mesmo tratamento do pipeline de IA do M06.**
  `MERCADOPAGO_ACCESS_TOKEN`/`MERCADOPAGO_WEBHOOK_SECRET` vazios por padrão
  (`application.yml`, mesmo bloco `sawhub.pagamento`, mesmo raciocínio do bloco `ia`) — checkout
  falha com `PagamentoIndisponivelException` clara em vez de tentar uma chamada fadada a 401.
  Verificado só até a borda; validar com credenciais reais (sandbox do Mercado Pago) antes de
  qualquer demo que dependa do checkout funcionar de fato.
- **Webhook com verificação de assinatura obrigatória desde o início**, não um "achado pra
  corrigir depois": Mercado Pago assina notificações (`x-signature`/`x-request-id`, HMAC-SHA256
  com o webhook secret) — `POST /api/v1/webhooks/mercadopago` rejeita qualquer notificação sem
  assinatura válida antes de processar. Além disso, o valor/status do pagamento NUNCA é confiado a
  partir do corpo da notificação — o webhook só usa o `payment id` recebido pra re-consultar o
  Payment de verdade na API do Mercado Pago (padrão documentado pelo próprio gateway contra
  notificações forjadas).
- **`PAGO` e `LIBERADO` transitam juntos, automaticamente, no mesmo webhook** — CLAUDE.md lista os
  dois como estados formalmente distintos da máquina, e o schema os mantém distintos (2 colunas de
  histórico reais), mas H8.3 descreve aprovação → "vira Pago" e "o item digital é liberado" na
  mesma frase, sem gate manual do Admin no meio. Catálogo é 100% digital nesta leva (ver abaixo) —
  se a SAW um dia vender algo que exija atendimento manual (produto físico, geração de licença),
  `LIBERADO` vira uma transição manual separada; até lá, automática.
- **Catálogo 100% digital — sem "produtos físicos" nesta leva.** O protótipo congelado
  (`design/prototipo/index.html`) mostra uma categoria "Produtos físicos" nos filtros, mas H8.3 só
  descreve entrega via "item digital é liberado" (sem fluxo de frete/rastreio/endereço). Categorias
  usadas são as de `spec.md` H8.1 (`CURSO, PLANILHA, TEMPLATE, EBOOK, FERRAMENTA, KIT,
  CONSULTORIA`), não as do protótipo — o protótipo é só referência visual de card, não a fonte de
  verdade de categorização (mesma hierarquia spec.md > protótipo já usada desde o M09).
- **"Avaliação" (H8.1) é um campo estático curado pelo Admin, não um sistema de reviews.** Nem o
  protótipo congelado nem nenhum módulo existente têm conceito de review/nota de mentorado — H8.1
  só pede "vejo... com preço e avaliação", não pede a jornada de AVALIAR. `avaliacaoMedia`
  (`BigDecimal`, opcional) é preenchido pelo Admin ao cadastrar o produto, igual "destaque"/"tag".
  Se a SAW quiser reviews de verdade (quem pode avaliar, quando, baseado em quê) isso é um H8.5
  não escrito ainda — não inventar aqui.
- **Um carrinho ativo por mentorado, do lado do servidor** (não localStorage): bate com a máquina
  de estado do `CLAUDE.md` (`Pedido: Carrinho → Aguardando pagamento → ...`) tratando "Carrinho"
  como um estado real e persistido do `Pedido`, não uma abstração cliente-only. Adicionar/remover
  item muta o MESMO `Pedido` em `CARRINHO`; finalizar compra transiciona esse pedido pra frente
  (nunca cria um novo). Sobrevive a troca de dispositivo/refresh — mais robusto que localStorage
  pra 10-15 usuários que podem comprar do celular e continuar no desktop.
- **Preço do item é travado no momento em que entra no carrinho** (`ItemPedido.precoUnitario`
  como snapshot), não recalculado no checkout — evita que uma mudança de preço pelo Admin durante
  o processo de compra altere o valor que o mentorado já viu no carrinho. Reação a um "produto já
  no carrinho" é incrementar quantidade, não re-snapshotar o preço.
- **Fecha o contrato já documentado desde o E13/E14**: `ROADMAP.md`'s Blueprint do E13 já avisa
  que `vendasLoja` fica em R$ 0 "até o E8 existir" e lê de `OrigemReceita.LOJA` — este módulo
  ESCREVE em `LancamentoFinanceiro` (categoria pré-semeada "Loja SAW", `TipoLancamento.RECEITA`,
  `StatusLancamento.REALIZADO`) no momento em que o pedido vira `PAGO`, seguindo o padrão já
  estabelecido (`ContaPagarReceberService.liquidar()`: sem service intermediário, o service do
  domínio salva o `LancamentoFinanceiro` direto). Não duplica contagem própria de receita — o
  Financeiro continua sendo o dono único do número, mesmo princípio já citado no Blueprint do E13.
- **RBAC do Admin: `Modulo.COMERCIAL`** (curadoria de `Produto` + reembolso/cancelamento manual de
  `Pedido`) — suposição explícita, `CLAUDE.md` não define qual área da SAW cuida da Loja; Comercial
  (vendas, funil, ranking) é o encaixe mais próximo do que existe hoje. Ajustar se o cliente
  confirmar outra área.
- **Reembolso é ação manual do Admin, não self-service do mentorado.** H8.3 descreve pagamento
  RECUSADO (falha no primeiro attempt, carrinho preservado) — isso é tratado automaticamente pelo
  checkout. Estornar um pedido JÁ PAGO é uma operação distinta (dinheiro de volta), que a SAW faz
  direto no painel do Mercado Pago hoje — o Admin só reflete isso no SAW HUB via
  `PATCH /admin/pedidos/{id}/reembolsar`, sem iniciar o estorno de verdade por aqui. Mesma
  categoria de pendência do M12/M13 (dado/endpoint existe, curadoria fina fica pra depois).
- **Sem nota fiscal.** `spec.md` já lista "emissão fiscal" como pendência aberta separada da
  escolha de gateway — não é parte de H8.1-H8.4, não entra nesta leva.
- **Correção de uma inconsistência encontrada no ROADMAP.md**: a nota de ordenação do E13
  ("Loja depende de Assinatura/Perfil (E9) existirem antes de fazer sentido") contradizia a
  própria tabela de pipeline (que já colocava E8 antes de E9). Não há dependência técnica real —
  H8.1-H8.4 não usam nada de H9.1-H9.3 (perfil/XP/assinatura são conceitos separados de "comprar um
  produto avulso"). Nota removida da tabela de pipeline; E8 segue na ordem em que já estava.

## Modelagem de banco (M14)

```sql
-- V11__loja.sql
CREATE TABLE produto (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo            VARCHAR(255) NOT NULL,
    descricao         TEXT NOT NULL,
    categoria         VARCHAR(20) NOT NULL,
    preco             NUMERIC(10,2) NOT NULL,
    preco_original    NUMERIC(10,2),
    avaliacao_media   NUMERIC(2,1),
    destaque          BOOLEAN NOT NULL DEFAULT false,
    vendas            INT NOT NULL DEFAULT 0,
    arquivo_url       VARCHAR(500) NOT NULL,
    imagem_url        VARCHAR(500),
    publicado         BOOLEAN NOT NULL DEFAULT false,
    criado_em         TIMESTAMP NOT NULL DEFAULT now(),
    versao            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_produto_categoria CHECK (categoria IN
        ('CURSO','PLANILHA','TEMPLATE','EBOOK','FERRAMENTA','KIT','CONSULTORIA')),
    CONSTRAINT chk_produto_preco CHECK (preco > 0)
);
CREATE INDEX idx_produto_categoria ON produto(categoria);
CREATE INDEX idx_produto_publicado ON produto(publicado);

CREATE TABLE pedido (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentorado_id          UUID NOT NULL REFERENCES mentorado(id),
    status                VARCHAR(25) NOT NULL DEFAULT 'CARRINHO',
    valor_total           NUMERIC(10,2) NOT NULL DEFAULT 0,
    referencia_gateway    VARCHAR(255),
    criado_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMP NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_pedido_status CHECK (status IN
        ('CARRINHO','AGUARDANDO_PAGAMENTO','PAGO','LIBERADO','CANCELADO','REEMBOLSADO'))
);
-- Só 1 carrinho ATIVO por mentorado — índice parcial, não constraint de tabela inteira (o
-- mentorado pode ter vários pedidos PAGO/LIBERADO no histórico, só não 2 CARRINHO ao mesmo tempo).
CREATE UNIQUE INDEX idx_pedido_carrinho_unico ON pedido(mentorado_id) WHERE status = 'CARRINHO';
CREATE INDEX idx_pedido_mentorado ON pedido(mentorado_id);

CREATE TABLE item_pedido (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id         UUID NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto_id        UUID NOT NULL REFERENCES produto(id),
    quantidade        INT NOT NULL,
    preco_unitario    NUMERIC(10,2) NOT NULL,
    criado_em         TIMESTAMP NOT NULL DEFAULT now(),
    versao            BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_item_pedido_quantidade CHECK (quantidade > 0)
);
CREATE UNIQUE INDEX idx_item_pedido_unico ON item_pedido(pedido_id, produto_id);
```

## Contratos de API (M14)

```jsonc
// --- Admin (Modulo.COMERCIAL) ---
POST/GET/PUT /api/v1/admin/produtos               // CRUD, mesmo padrão de Conteudo (M06/M11)
PATCH /api/v1/admin/produtos/{id}/publicar
PATCH /api/v1/admin/produtos/{id}/despublicar
PATCH /api/v1/admin/pedidos/{id}/reembolsar        // manual, ver Suposições acima
PATCH /api/v1/admin/pedidos/{id}/cancelar

// --- Mentorado (hasRole MENTORADO) ---
GET /api/v1/mentorado/loja/produtos?categoria=&busca=&destaque=
[{ "id":"uuid","titulo":"Pacote de Planilhas Gerenciais","categoria":"PLANILHA",
   "preco":97.00,"precoOriginal":197.00,"avaliacaoMedia":4.8,"destaque":true,
   "vendas":42,"imagemUrl":"https://...", "publicado":true }]
// só publicado=true, nunca arquivoUrl (só liberado após pagamento, ver ItemPedidoResponse abaixo)

GET /api/v1/mentorado/loja/carrinho
// Devolve o Pedido em CARRINHO do mentorado autenticado, ou um carrinho vazio (sem criar linha
// no banco até o 1º item ser adicionado — carrinho vazio não é um Pedido real).
{ "id":"uuid","status":"CARRINHO","valorTotal":97.00,
  "itens":[{ "id":"uuid","produtoId":"uuid","titulo":"...","quantidade":1,"precoUnitario":97.00 }] }

POST /api/v1/mentorado/loja/carrinho/itens
{ "produtoId":"uuid","quantidade":1 }
// Cria o carrinho se não existir. Item já no carrinho: soma quantidade, NÃO re-snapshota preço.

PATCH /api/v1/mentorado/loja/carrinho/itens/{itemId}
{ "quantidade":2 }

DELETE /api/v1/mentorado/loja/carrinho/itens/{itemId}

POST /api/v1/mentorado/loja/checkout
// 409 se o carrinho estiver vazio ou algum produto tiver sido despublicado desde que entrou no
// carrinho. 503 (PagamentoIndisponivelException) se MERCADOPAGO_ACCESS_TOKEN não configurado.
{ "checkoutUrl": "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=..." }
// Pedido: CARRINHO -> AGUARDANDO_PAGAMENTO nesta chamada.

GET /api/v1/mentorado/loja/pedidos
// Histórico — todo Pedido do mentorado que NÃO está em CARRINHO, mais recente primeiro.
[{ "id":"uuid","status":"LIBERADO","valorTotal":97.00,"criadoEm":"...",
   "itens":[{ "titulo":"...","quantidade":1,"arquivoUrl":"https://..." }] }]
// arquivoUrl só aparece quando status=LIBERADO — mesmo raciocínio do "ata RASCUNHO nunca aparece"
// do M12: dado sensível ao estado, não uma checagem de front-end.

// --- Webhook público, sem sessão (verificado por assinatura, não por hasRole) ---
POST /api/v1/webhooks/mercadopago
// 401 se a assinatura x-signature não bater. 200 sempre que processado (mesmo se o pedido já
// estava PAGO — idempotente, notificação duplicada não reprocessa).
```

## Rastreabilidade história ↔ módulo (M14)

| História | Cobertura |
|---|---|
| H8.1 — navegar catálogo por categoria, destaques/mais vendidos | `GET /mentorado/loja/produtos?categoria=&destaque=` (`vendas` cobre "mais vendidos", `avaliacaoMedia` estática — ver Suposições) |
| H8.2 — adicionar ao carrinho, subtotal atualiza | `POST/PATCH/DELETE /mentorado/loja/carrinho/itens`, `valorTotal` no `GET /carrinho` |
| H8.3 — checkout seguro, aprovado libera item, recusado preserva carrinho | `POST /checkout` + webhook assinado; recusa não transiciona o Pedido, carrinho intacto por construção |
| H8.4 — pedido com trilha de estado | Schema com status formal + `PATCH` admin pra Cancelado/Reembolsado |

**Status: ✅ M14 concluído** (2026-07-09) — backend (270/270 testes, incluindo `PedidoTest` (máquina
de estado + matemática do carrinho, sem mocks), `LojaMentoradoServiceTest`, `PedidoPagamentoServiceTest`
(idempotência do webhook, integração com Financeiro), `MercadoPagoGatewayServiceTest` (verificação
de assinatura HMAC recomputada de forma independente), `PedidoAdminServiceTest`) + frontend
(`LojaPage` — catálogo/carrinho/meus pedidos — e `ProdutosPage`/`PedidosPage` no Admin) + E2E
(`loja.spec.ts`, 6 testes) — **52/52 verde na suíte completa**.

**Achado ao vivo, antes do `revisor-seguranca`:** checkout sem `MERCADOPAGO_ACCESS_TOKEN`
configurado (esperado neste ambiente) estourava 500 genérico em vez de erro claro —
`PagamentoIndisponivelException` nunca tinha um handler em `GlobalExceptionHandler` (mesma classe
de achado do H14/M06: erro de negócio/dependência externa fora do ar vs. erro interno). Corrigido
com handler novo → 503, mensagem clara ("MERCADOPAGO_ACCESS_TOKEN não configurado — checkout
indisponível.").

**`revisor-seguranca` (mandatório, mesmo tratamento do Auth): Seguro** — nenhuma falha
crítica/alta explorável nos caminhos de dinheiro, webhook ou isolamento de tenant. Toda a cadeia
preço→carrinho→checkout→webhook→Financeiro foi rastreada: nenhum número vindo do corpo da
requisição do cliente (só `produtoId`/`quantidade`) alimenta o valor cobrado ou lançado — o preço
sempre vem de `Produto.preco` travado no servidor no momento do add-to-cart. Webhook nunca confia
no corpo da notificação, sempre re-consulta o Payment real na API do Mercado Pago a partir só do
id. Dois achados de hardening aplicados (não bloqueantes, mas corrigidos por ser módulo de
pagamento): (1) `quantidade` sem teto superior — adicionado `@Max(20)`; (2) assinatura do webhook
sem checagem de frescor do timestamp (janela de replay) — adicionado limite de 5 minutos. Dois
achados registrados como pendência, ver abaixo.

**Pendências reais, documentadas, não escondidas:** (1) **validar contra o sandbox real do
Mercado Pago antes de qualquer demo/produção** — a verificação de assinatura HMAC foi implementada
de boa-fé a partir da documentação pública (mesmo "verificado só até a borda" do pipeline de IA do
M06), nunca testada contra um webhook de verdade; (2) sem rate limiting em `POST
.../loja/checkout` — cada chamada gera uma Preference real na API do Mercado Pago, um mentorado
autenticado pode chamar repetidamente (custo/abuso de API, não vazamento de dado — baixo risco
dado que exige sessão); (3) `PedidoAdminService.reembolsar()`/`cancelar()` só refletem o estado no
SAW HUB, nunca chamam a API de estorno de verdade do Mercado Pago — decisão documentada desde o
Blueprint, não uma lacuna; (4) sem nota fiscal (fora de escopo desde o Blueprint, `spec.md` já
lista como pendência separada da escolha de gateway).

## Blueprint (M15 · E9 · Perfil & Gamificação)

**Por que Médio, sem `revisor-seguranca` obrigatório por CLAUDE.md (mas revisado do mesmo jeito,
por convenção desta esteira):** módulo mentee-facing self-service, mesmo formato de M08-M13 — sem
dinheiro (H8/M14 já é o único módulo de risco alto do lado mentorado), sem integração externa nova.
Risco real é o de sempre (isolamento por tenant num endpoint de escrita nova), não um risco de
categoria alta como Auth/Pagamento.

**Leitura do mockup congelado** (`design/prototipo/uploads/05-loja-perfil.png`, tela "Meu perfil"):
cartão de identidade (foto, nome, cargo/negócio, bio, contato, tags de "áreas de interesse", botão
"Editar perfil"), abas (Visão geral / Preferências / Segurança / Notificações / Integrações — só
"Visão geral" tem conteúdo desenhado, as demais são placeholders visuais sem wireframe detalhado),
bloco "Minha jornada SAW" (nível atual com ícone, barra de progresso pro próximo nível, 4
indicadores: materiais acessados / dicas assistidas / eventos participados / conquistas),
"Conquistas recentes" (badges com data de desbloqueio), barra lateral "Resumo da conta" (plano,
vencimento, contadores, botão "Gerenciar plano") e "Minha assinatura" (plano ativo, próxima
cobrança).

**Suposições (decisões conscientes, documentadas, não escondidas):**
1. **Só a aba "Visão geral" entra nesta leva.** Preferências/Segurança/Notificações/Integrações
   não têm wireframe no mockup (nenhum campo detalhado) nem história correspondente em `spec.md`
   além do que H9.1 já cobre (editar dados/preferências básicas) — construir abas vazias seria
   forçar escopo que o cliente nunca especificou. "Preferências" que H9.1 pede (bio, telefone,
   áreas de interesse, foto) entra dentro da própria Visão geral, no cartão de identidade, mesmo
   padrão do mockup (não há uma aba "Preferências" separada nele — o botão único é "Editar perfil").
2. **XP não é event-sourced/persistido — é derivado por fórmula, recalculado a cada leitura**, a
   partir de contadores que já existem hoje: materiais acessados/dicas assistidas
   (`ConteudoMentorado`, M11), eventos participados (`InscricaoEvento.status=PARTICIPOU`, M13),
   mentorias realizadas (`Mentoria.status=REALIZADA`, M12), metas concluídas (`Meta.status=
   CONCLUIDA`, M09), tarefas concluídas (`Encaminhamento/Tarefa.status=CONCLUIDA`, M10). Decisão
   consciente pra **não** retrofitar side-effects de "ganhar XP" dentro de 5 módulos já fechados,
   testados e revisados nesta esteira (M09-M13) — inserir um incremento de XP em cada fluxo já
   fechado é editar código estável sem necessidade (contraria a Diretiva Primária do `CLAUDE.md`) e
   arrisca reabrir regressão em módulos com `revisor-seguranca` já aprovado. Se o cliente quiser XP
   com histórico de eventos (ex.: "quando ganhei cada ponto"), é trabalho adicional documentado à
   parte, não implícito nesta leva.
3. **Conquistas (badges) também são calculadas ao vivo por limiar, sem data de desbloqueio
   persistida** — mesma razão do item 2 (sem histórico de eventos, não dá pra saber quando o
   limiar foi cruzado). O mockup mostra "Conquistado em DD/MM/AAAA"; aqui a conquista aparece
   como desbloqueada/bloqueada, sem data. Documentado como lacuna real, não escondida.
4. **"Vencimento do plano" é um campo novo, setado pelo Admin** (estende `AtualizarMentoradoRequest`
   existente do M02/E15), não uma regra de cobrança recorrente calculada — não existe hoje nenhuma
   integração de assinatura recorrente com o Mercado Pago (M14/E8 cobriu só compra avulsa da Loja).
   Seedado com uma data prevista pra cada mentorado de demonstração.
5. **"Upgrade/downgrade" (H9.3) é informativo, não self-service com cobrança.** O mentorado vê o
   plano atual, o vencimento e a lista de planos disponíveis (nome + posição na hierarquia); mudar
   de plano de fato continua sendo uma ação do Admin (`PUT /admin/mentorados/{id}`, já existe desde
   M02). Trocar de plano com prorateamento e cobrança automática de assinatura recorrente é uma
   funcionalidade bem maior (gateway de assinatura recorrente, não coberto por `spec.md` H9.3, que
   só pede "vejo vencimento e opções de upgrade/downgrade" — ver, não necessariamente executar).
   Documentado aqui pra não virar suposição silenciosa depois.
6. **"Áreas de interesse" é texto livre (CSV), sem taxonomia fixa.** `spec.md` H9.1 só diz "editar
   dados/preferências", sem especificar uma lista fechada de áreas — diferente do RBAC do E15
   (`Modulo` enum), que é uma classificação de acesso administrativo, não um dado de perfil do
   mentorado. Misturar os dois conceitos seria errado.
7. **Foto de perfil é uma URL externa (`fotoUrl`), não upload de arquivo** — mesmo padrão já
   usado em `Conteudo.url`/`Produto.imagemUrl` (M11/M14), sem nova infraestrutura de upload de
   imagem pra este módulo.

**Modelagem de banco (M15 — só ALTER, nenhuma tabela nova):**

```sql
ALTER TABLE mentorado ADD COLUMN telefone VARCHAR(30);
ALTER TABLE mentorado ADD COLUMN bio VARCHAR(500);
ALTER TABLE mentorado ADD COLUMN areas_interesse VARCHAR(300);
ALTER TABLE mentorado ADD COLUMN foto_url VARCHAR(500);
ALTER TABLE mentorado ADD COLUMN vencimento_plano DATE;
```

Sem tabela nova pra XP/conquistas — ambos calculados em memória a partir de dados já existentes
(item 2/3 das Suposições), zero risco de nova race condition ou índice.

**Fórmula de XP (documentada, ajustável):**

```
xp = materiaisAcessados*10 + dicasAssistidas*15 + eventosParticipados*150
   + mentoriasRealizadas*200 + metasConcluidas*100 + tarefasConcluidas*15
```

`NivelJornada`: `BRONZE (0)`, `PRATA (1500)`, `OURO (4000)`, `DIAMANTE (8000)` — nível = maior
patamar cujo `xpMinimo` ≤ xp atual; progresso = `xp` normalizado entre o patamar atual e o próximo
(Diamante não tem "próximo nível", barra fica cheia).

**Conquistas (limiares fixos, sem persistência):** Primeiro Evento (eventos≥1) · Mentoria Realizada
(mentorias≥1) · Maratonista (materiais≥10) · Sempre Ligado (dicas≥5) · Meta Batida (metas
concluídas≥1) · Produtivo (tarefas concluídas≥10) · Em Crescimento (`crescimentoFaturamentoPct`>0)
· Ferramentas em Dia (`ferramentasConcluidas == ferramentasTotal`, `ferramentasTotal`>0).

**Contratos de API:**

```
GET /api/v1/mentorado/perfil
// Response 200
{
  "nome": "Ana Costa", "negocio": "Ana Costa Restaurante", "email": "ana@anacosta.com.br",
  "telefone": "(11) 98765-4321", "bio": "...", "areasInteresse": ["Gestão", "Finanças"],
  "fotoUrl": "https://...", "plano": "ESSENCIAL", "vencimentoPlano": "2026-09-15",
  "membroDesde": "2026-01-10T12:00:00Z"
}

PATCH /api/v1/mentorado/perfil
// Request — só campos de auto-edição (nome/negócio/plano continuam admin-only)
{ "telefone": "...", "bio": "...", "areasInteresse": ["Gestão"], "fotoUrl": "https://..." }
// Response 200 — mesmo shape do GET

GET /api/v1/mentorado/perfil/jornada
// Response 200
{
  "nivelAtual": "OURO", "xp": 4820, "xpProximoNivel": 8000, "progressoPct": 62,
  "stats": { "materiaisAcessados": 12, "dicasAssistidas": 6, "eventosParticipados": 1, "mentoriasRealizadas": 2 },
  "conquistas": [ { "codigo": "PRIMEIRO_EVENTO", "titulo": "Primeiro Evento", "descricao": "...", "desbloqueada": true } ]
}

GET /api/v1/mentorado/perfil/assinatura
// Response 200
{
  "planoAtual": "ESSENCIAL", "vencimentoPlano": "2026-09-15",
  "planosDisponiveis": [ { "plano": "PROFISSIONAL", "label": "Profissional", "acimaDoPlanoAtual": true } ]
}
```

Validação: `telefone` ≤30 chars, `bio` ≤500 chars, `fotoUrl` opcional com
`@Pattern(regexp="^https?://.+")` (mesmo padrão de `Conteudo.url`/`Produto.imagemUrl`),
`areasInteresse` lista de string, cada uma ≤50 chars, lista ≤10 itens.

**Rastreabilidade:**

| História | Cobertura |
|---|---|
| H9.1 — ver e editar perfil | `GET/PATCH /mentorado/perfil` |
| H9.2 — ver jornada e conquistas | `GET /mentorado/perfil/jornada` |
| H9.3 — gerenciar assinatura (vencimento + opções) | `GET /mentorado/perfil/assinatura` |

**Status: ✅ M15 concluído** (2026-07-09) — backend (280/280 testes, incluindo `NivelJornadaTest`
(fronteiras de patamar), `PerfilMentoradoServiceTest` (isolamento por tenant, campos admin-only
intocados por `atualizar()`), `PerfilJornadaServiceTest` (fórmula de XP, limiares de conquistas,
progresso no topo da escala)) + frontend (`PerfilPage` — cartão de identidade/edição, jornada com
barra de progresso e conquistas, sidebar de assinatura) + E2E (`perfil.spec.ts`, 5 testes) —
**57/57 verde na suíte completa**.

**Achado do próprio Blueprint, corrigido antes do `revisor-seguranca` fechar a revisão:** a
Suposição 4 do Blueprint dizia que `vencimentoPlano` "estende `AtualizarMentoradoRequest`
existente do M02/E15", mas a implementação inicial só tocou o `Mentorado`/seeder — o formulário
admin de edição (`MentoradosListaPage.tsx`) nunca ganhou o campo. Além de ficar incoerente com o
Blueprint, isso era um bug real: salvar nome/negócio/plano pelo Admin sem o campo silenciosamente
zerava o `vencimentoPlano` que já existisse (o backend sempre grava o valor recebido, e o campo
nunca era enviado). Corrigido: `AtualizarMentoradoRequest`/`MentoradoResponse` ganharam o campo,
`MentoradoAdminService.atualizar()` grava `vencimentoPlano`, e o formulário admin ganhou o input de
data — verificado via curl que um `PUT` sem tocar o campo agora preserva o valor.

**`revisor-seguranca` (mesmo tratamento de todo módulo desta esteira): Seguro** — isolamento por
tenant confirmado nos 4 endpoints (sempre via `findByUsuarioId(principal.getUsuarioId())`, nunca
parâmetro de request), `PerfilJornadaService.jornada()` auditado ponta a ponta (as 5 agregações
usam o id do mesmo mentorado resolvido, sem vazamento cross-tenant), `AtualizarPerfilMentoradoRequest`
confirmado incapaz de alterar nome/negócio/plano, `fotoUrl` com `@Pattern` bloqueando esquemas
perigosos e renderizada só como atributo `<img src>` (sem risco de XSS via atributo, sem SSRF —
backend nunca busca a URL), `bio`/`areasInteresse` renderizados como texto puro em React (sem
`dangerouslySetInnerHTML`) com limites de tamanho aplicados via Bean Validation batendo com as
colunas do `V12`. Nenhum achado bloqueante — quarta revisão limpa desde M08-M10/M13.

**Pendência real, documentada, não escondida:** XP/nível/conquistas são 100% calculados por
leitura (ver Suposições 2/3 do Blueprint acima) — não há histórico de eventos, então não é possível
mostrar "conquistado em DD/MM" como o mockup sugere, e o XP não é ajustável manualmente. Se o
cliente quiser XP com histórico real de eventos, é trabalho adicional (event sourcing), não uma
extensão trivial deste módulo. Sem pendência de credencial externa.

## Blueprint (M16 · E10 · Painel Administrativo & Métricas)

**Por que Médio, sem `revisor-seguranca` obrigatório por CLAUDE.md (revisado do mesmo jeito, por
convenção desta esteira):** agregação pura de leitura sobre dados que já existem (Mentorado,
Mentoria, Evento, Conteudo, Financeiro/E14) — mesma classe de módulo que E17 Painel Consolidado e
o Dashboard Comercial (E13), sem entidade nova, sem escrita nova, sem dado sensível novo. É o
"KPI geral" do Admin — `spec.md` já deixa explícito que isso é diferente do E17 (consolidado por
mentorado) e do E13/E14 (dashboards por área), não uma sobreposição.

**Estado de partida, diferente de todo módulo anterior:** `/admin/dashboard` e `Modulo.DASHBOARD`
**já existem como scaffolding** desde antes desta leva — a rota está protegida por
`RequireModulo modulo="DASHBOARD"` e é a PRIMEIRA da ordem de redirecionamento pós-login
(`MODULO_ROUTE_ORDER` em `moduloRoutes.ts`), só que hoje renderiza `<PlaceholderScreen
title="Dashboard" />`. Este módulo substitui o placeholder pelo conteúdo real — sem RBAC novo,
sem rota nova.

**Leitura do mockup congelado** (`design/prototipo/uploads/06-admin.png`, "Tela 11 — Dashboard
Administrativo"): 4 cartões de KPI com variação % (mentorados ativos, mentorias realizadas,
eventos realizados, receita do mês), gráfico de linha "Crescimento de mentorados" (últimos 6
meses), donut "Distribuição por plano", lista "Atividades recentes", lista "Mentorias agendadas
para hoje".

**Suposições (decisões conscientes, documentadas, não escondidas):**
1. **"Atividades recentes" deriva de timestamps de CRIAÇÃO já existentes** (`Mentorado.criadoEm`,
   `Evento.criadoEm`, `Conteudo.criadoEm` quando publicado) — o sistema não tem tabela de
   auditoria/log de eventos. Não cobre transições de status (ex.: "mentoria concluída" do mockup)
   porque nenhuma entidade rastreia a DATA da transição, só o status atual — mesma limitação já
   aceita no E14 (DRE não tem histórico de mudança de status de conta). Mesma classe de decisão do
   XP derivado do M15: dado computado por leitura, não persistido, sem inventar uma tabela de
   evento só pra esta tela.
2. **"Crescimento de mentorados" (6 meses) e a variação % de "mentorados ativos" usam
   `Mentorado.criadoEm` como proxy de crescimento de base**, contando cadastros acumulados até o
   fim de cada mês — não há histórico de ativar/desativar, então "ativos naquele mês passado" é
   aproximado pelo total cadastrado até lá (se alguém foi desativado depois, o gráfico de meses
   anteriores não reflete retroativamente). Documentado, não escondido.
3. **"Mentorias realizadas"/"Eventos realizados" no mês usam a data do evento (`dataHora`) como
   mês de referência**, não uma data de transição de status separada (que não existe).
4. **Centralização de dívida técnica encontrada, não introduzida por este módulo:**
   `variacaoPct(BigDecimal anterior, BigDecimal atual)` já está duplicado de forma idêntica em
   `RelatorioFinanceiroService` (E14) e `ComercialDashboardService` (E13) — este módulo seria o
   TERCEIRO a reimplementar a mesma fórmula. Extraído pra `common/VariacaoCalculator` (com um
   overload `long`/`long` novo, usado pelos KPIs de contagem deste módulo) e os dois call sites
   existentes apontados pra lá — mesma disciplina do `ProgressoCalculator`/`Plano.atendePlanoMinimo`
   (lições do M08/M11): centralizar no momento em que a duplicata é encontrada, não adiar.

**Contratos de API:**

```
GET /api/v1/admin/dashboard?ano=&mes=   // default: mês corrente
// Response 200
{
  "mentoradosAtivos": 6, "variacaoMentoradosAtivosPct": 20.0,
  "mentoriasRealizadas": 2, "variacaoMentoriasRealizadasPct": 0.0,
  "eventosRealizados": 1, "variacaoEventosRealizadosPct": 0.0,
  "receitaMes": 1770.00, "variacaoReceitaMesPct": 12.5,
  "crescimentoMentorados": [ { "mes": "2026-02", "total": 4 }, ... 6 itens ],
  "distribuicaoPlano": [ { "plano": "ESSENCIAL", "quantidade": 3, "pct": 50.0 }, ... ],
  "atividadesRecentes": [
    { "tipo": "MENTORADO_CADASTRADO", "descricao": "Novo mentorado: Ana Costa", "quando": "2026-07-09T00:37:06Z" }
  ],
  "mentoriasHoje": [
    { "tipo": "INDIVIDUAL", "mentorNome": "Brayan", "mentoradoNomes": "João Silva", "hora": "10:00", "status": "CONFIRMADA" }
  ]
}
```

**Rastreabilidade:**

| História | Cobertura |
|---|---|
| H10.1 — visão geral (mentorados ativos, mentorias/eventos realizados, receita do mês, variação) | 4 KPIs do `GET /admin/dashboard` |
| H10.2 — crescimento e distribuição por plano | `crescimentoMentorados` + `distribuicaoPlano` |
| H10.3 — atividades recentes e mentorias do dia | `atividadesRecentes` + `mentoriasHoje` |

**Status: ✅ M16 concluído** (2026-07-10) — backend (290/290 testes, incluindo
`DashboardAdminServiceTest` (contagem de ativos, distribuição por plano, crescimento de 6 meses,
mentorias/eventos realizados por mês, mentorias de hoje, atividades recentes ordenadas/limitadas)
e `VariacaoCalculatorTest`) + frontend (`DashboardAdminPage` substitui o `PlaceholderScreen` que
ocupava `/admin/dashboard` desde antes desta leva) + E2E (`dashboard-admin.spec.ts`, 2 testes) —
**59/59 verde na suíte completa**.

**Refatoração feita antes do código novo (não depois):** `variacaoPct` já estava duplicado de
forma idêntica em `RelatorioFinanceiroService` (E14) e `ComercialDashboardService` (E13) — this
módulo seria o terceiro ponto a reimplementar a mesma fórmula. Extraído pra
`common/VariacaoCalculator` (com overload `long`/`long` novo pros KPIs de contagem) antes de
escrever `DashboardAdminService`, não como um achado de `revisor-seguranca` corrigido depois —
mesma disciplina do `ProgressoCalculator`/`Plano.atendePlanoMinimo` (M08/M11).

**`revisor-seguranca` (mesmo tratamento de todo módulo desta esteira): Seguro** — confirmado que
`Modulo.DASHBOARD` só está em `AreaModuloMatrix.FUNDADOR` (nenhuma outra área vê o módulo, testado
ao vivo com um usuário Comercial retornando 403), agregação admin-wide correta pra este caso (não
é vazamento cross-tenant, é o propósito do módulo), "atividades recentes" só expõe nome/título
(nunca email/telefone), `ano`/`mes` validados sem caminho de exceção não tratada, refatoração do
`VariacaoCalculator` confirmada comportamentalmente idêntica ao código antigo. Nenhum achado —
quinta revisão limpa desde M08-M10/M13/M15.

**Pendência real, documentada, não escondida:** "atividades recentes" não cobre transições de
status (ex.: "mentoria concluída", que o mockup mostra) porque nenhuma entidade rastreia a DATA da
transição, só o status atual — só cobre eventos de criação (mentorado cadastrado, evento criado,
conteúdo publicado). "Crescimento de mentorados" e a variação de "mentorados ativos" usam
`criadoEm` como proxy (sem histórico de ativar/desativar). Sem pendência de credencial externa.

## Fórmula de prazo

```
Prazo = Fase 2 (2d ou 4d se sem identidade) + Σ(dias dos módulos) + 2d (Fase 5)
```

Fase 2 já entregue (protótipo aprovado e congelado). M04 (E14, concluído) somou **6 dias** de
engenharia; M05 (E13, concluído) somou mais **6d + ~1d** (o dia extra cobriu o fast-follow de H1.3,
achado faltando durante o Blueprint do M05); M06 (E11+E5+IA, concluído) somou mais **6d + ~2-3d**
(a variação cobriu o pipeline de IA — transcrição + LLM + revisão humana — que não tinha precedente
nos módulos anteriores pra calibrar a estimativa com mais precisão) — os módulos seguintes
(mentorado) somam seus próprios dias quando ganharem Blueprint, na ordem em que entrarem.

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
| 3 | E11 · Gestão Admin (mentorias ind./grupo, curadoria, eventos) + E5 · Mentorias & Atas (lado Admin) + **diferencial de IA** (transcrição de áudio → rascunho de ata) | Grande | 6d + ~2-3d da integração de IA | ✅ Concluído — backend (137/137 testes) + `revisor-seguranca` (1 alto/2 médios/1 baixo corrigidos) + frontend (mentorados/mentorias/ata/conteúdos/eventos) + E2E (21/21, `mentorados.spec.ts`). Pipeline de IA verificado até a borda (falha limpa sem `OPENAI_API_KEY`/`ANTHROPIC_API_KEY`) — validar com chaves reais antes da demo. H5.1-H5.3 (Mentorias) e H7.1-H7.3 (Eventos) são histórias do mentorado (não do Admin) — dado/CRUD pronto aqui, tela deferida pra quando os módulos do mentorado entrassem no pipeline; ver linha 9 (M12) e linha 10 (M13) |
| 4 | Google OAuth (fast-follow do E1) | Pequeno | 1.5d | ✅ Concluído — backend (141/141 testes) + `revisor-seguranca` (2 achados corrigidos: oráculo de enumeração de contas + nota pendurada) + frontend (botão condicional + tradução de erro) + E2E (24/24, `google-oauth.spec.ts`). Fluxo real (redirect→consentimento→callback) verificado só até a borda — sem app Google Cloud Console configurado neste ambiente |
| 5 | E2 · Dashboard do Mentorado | Médio | 3.5d | ✅ Concluído — backend (152/152 testes) + `revisor-seguranca` (sem achado bloqueante, isolamento por tenant confirmado) + frontend (primeira rota `/mentorado` de verdade) + E2E (29/29, `dashboard-mentorado.spec.ts`) |
| 6 | E3 · Metas Estratégicas | Médio | 3.5d | ✅ Concluído — backend (165/165 testes, 1ª entidade nova desde o M06) + `revisor-seguranca` (sem achado bloqueante, isolamento por tenant confirmado nos caminhos de escrita) + frontend (`MetasPage` self-service) + E2E (32/32, `metas.spec.ts`) |
| 7 | E4 · Tarefas & Agenda | Médio | 3.5d | ✅ Concluído — backend (178/178 testes) + `revisor-seguranca` (sem achado bloqueante: peso do ranking protegido, isolamento Tarefa→Meta confirmado, migração idempotente, máquina de estado com guardas, JPQL seguro) + frontend (`TarefasPage` self-service) + E2E (35/35, `tarefas.spec.ts`) |
| 8 | E6 · Materiais & Dicas do Brayan | Médio | 3.5d | ✅ Concluído — backend (183/183 testes) + `revisor-seguranca` (4 achados corrigidos: reverse tabnabbing, `Plano.ordinal()` duplicado, corrida de criação concorrente, `url` sem validação de esquema) + frontend (`MateriaisPage`) + E2E (38/38, `materiais.spec.ts`). Indicadores agregados de consumo (H6.3) não implementados — pendência real |
| 9 | E5 · Mentorias & Atas (lado mentorado) | Médio | 4d | ✅ Concluído — backend (213/213 testes) + `revisor-seguranca` (2 achados corrigidos: injeção de CR solto no .ics, `linkOnline` sem validação de esquema) + frontend (`MentoriasPage`) + E2E (42/42, `mentorias.spec.ts`). Fecha H5.1-H5.3, deferidas desde o M06. Achado ao vivo: `LazyInitializationException` na listagem Admin, corrigido. Pendência: UI de curadoria de materiais recomendados no Admin (endpoint existe, tela não) |
| 10 | E7 · Eventos & Inscrições (lado mentorado) | Médio | 4.5d | ✅ Concluído — backend (226/226 testes) + `revisor-seguranca` (sem achado bloqueante — primeira revisão limpa desde M08-M10) + frontend (`EventosMentoradoPage`, calendário próprio) + E2E (46/46, `eventos.spec.ts`). Fecha H7.1-H7.3, deferidas desde o M06. Nova entidade `InscricaoEvento` com corrida de última vaga protegida por `@Version`. Pendência: janela de corrida rara em `marcarParticipacoes` (baixo impacto) |
| 11 | E8 · Loja SAW (catálogo, carrinho, checkout, gateway) | Grande · risco alto | 6d | ✅ Concluído — backend (270/270 testes) + `revisor-seguranca` obrigatório (Seguro — 2 achados de hardening corrigidos: teto de quantidade, janela de frescor da assinatura do webhook) + frontend (`LojaPage`, `ProdutosPage`/`PedidosPage` Admin) + E2E (52/52, `loja.spec.ts`). Gateway Mercado Pago (Checkout Pro), sem credencial neste ambiente — verificado só até a borda, validar contra sandbox real antes de produção |
| 12 | E9 · Perfil & Gamificação | Médio | 3.5d | ✅ Concluído — backend (280/280 testes) + `revisor-seguranca` (sem achado bloqueante — quarta revisão limpa da esteira) + frontend (`PerfilPage`) + E2E (57/57, `perfil.spec.ts`). XP/nível/conquistas calculados por leitura, sem persistência (ver Blueprint) — pendência real: sem data de desbloqueio de conquista, sem histórico de XP. Achado e corrigido: gap entre o Blueprint (vencimentoPlano via admin) e a implementação inicial (nunca fazia isso) |
| 13 | E10 · Painel Administrativo & Métricas (parte além do E17, já pronto) | Médio | 3.5d | ✅ Concluído — backend (290/290 testes) + `revisor-seguranca` (sem achado bloqueante — quinta revisão limpa da esteira) + frontend (`DashboardAdminPage`, substitui o placeholder que ocupava `/admin/dashboard`) + E2E (59/59, `dashboard-admin.spec.ts`). Refatoração proativa: `variacaoPct` (duplicado em E13/E14) centralizado em `VariacaoCalculator` antes do código novo. Pendência: "atividades recentes" só cobre eventos de criação, sem histórico de transição de status |
| 14 | E16 · Avisos & Notificações (transversal) | Pequeno | 1.5d | ⬜ |
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
natural na sequência do núcleo do mentorado (correção: uma versão anterior desta nota dizia que
Loja "dependia de Assinatura/Perfil (E9) existirem antes" — checado no Blueprint do M14 e não é
verdade, H8.1-H8.4 não usam nada de E9; assinatura/XP/conquistas são conceitos separados de
comprar um produto avulso).
