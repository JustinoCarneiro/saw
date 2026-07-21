# SAW HUB

Plataforma SaaS de mentoria para restaurantes: centraliza a jornada do mentorado (agenda, metas, tarefas, atas, conteúdos, eventos e loja) e dá à SAW um painel de operação e métricas.

## Stack
- **Frontend:** React (Vite) + TypeScript · SPA sem SSR — app é majoritariamente logado (dashboard/admin), não precisa de SEO.
- **Backend:** Java + Spring Boot (Spring MVC, Spring Data JPA/Hibernate).
- **Banco:** PostgreSQL (relacional) + Redis (cache, sessão).
- **Auth:** Spring Security + OAuth2 Client (e-mail/senha + Google) · perfis Mentorado e Admin, com RBAC por área dentro do Admin (ver E15).
- **Agendamento:** Spring `@Scheduled`/Quartz (e-mails, lembretes de mentoria) — substitui fila assíncrona pesada, os casos de uso são jobs agendados, não fila de alto volume.
- **Pagamento:** gateway externo (Mercado Pago / Stripe / Pagar.me) na Loja.
- **Deploy:** Hostinger VPS (KVM 2, 8GB) via **Coolify** (PaaS self-hosted, deploy por git push) — front (React) e back (Spring Boot, Docker) centralizados no mesmo servidor. Ver "Hospedagem" abaixo.
- **Protótipo (Fase 2b):** front 100% estático com dados fictícios, componentizado, para aprovação do cliente — sem backend. Stack acima só se aplica a partir da Fase 4; nenhuma linha de backend foi escrita ainda, então a troca de stack não tem retrabalho.

## Hospedagem
Decisão: **Hostinger VPS (KVM 2) com Coolify**, front e back no mesmo servidor — não Vercel/Railway/Render.
- **Por quê:** público 100% Brasil (CDN global da Vercel não traz ganho relevante); front e back juntos elimina o hop de rede entre eles nas chamadas da API; custo bem menor (~R$ 35–55/mês vs ~R$ 150–300/mês de PaaS gerenciado); mesmo modelo já validado em outro projeto da Onda (Sanarys/RecallMed, Spring Boot + Hostinger).
- **Trade-off aceito:** VPS é *self-managed* — a Onda instala e cuida de Postgres, Redis, TLS, atualizações e **verifica os backups** (o plano da Hostinger inclui backup automático, mas a checagem é manual). Sem alta disponibilidade nativa: se a VPS cair, front e back caem juntos.
- **Coolify** cobre a maior parte do que se perderia sem um PaaS gerenciado: deploy via git push, build isolado do processo que atende tráfego (sem downtime), TLS automático (Let's Encrypt), preview por branch.
- Migração pra AWS (ou outra infra com HA) fica prevista para quando a escala justificar — não é MVP.
- **Pendência de titularidade (2026-07-16):** a VPS atual é compartilhada com outros projetos da Onda (não é exclusiva do SAW HUB) e está na conta pessoal/Onda, não do cliente. Decisão tomada: a hospedagem também precisa ser propriedade do cliente (Onda continua operando). Isso exige migrar o SAW HUB pra uma VPS Hostinger nova e dedicada, na conta do cliente — não dá pra só transferir titularidade de uma VPS compartilhada. Plano de migração detalhado fora do repositório (arquivo local, contém dados de conta).

## Segurança e persistência dos dados (dúvida recorrente do cliente)
O cliente comparou o projeto com ferramentas no-code (ex.: Base44) e perguntou sobre risco de perder dados.
- O sistema (Fase 4) usa **Postgres em VPS própria** (Hostinger, com backup automático) — a SAW é dona do banco, do código e do servidor, não depende da sobrevivência de uma ferramenta no-code de terceiros.
- **Java + Spring Security** é a escolha para o backend justamente pela maturidade em regras de permissão complexas (RBAC por área no E15) — não é só uma preferência de conforto, é o ecossistema mais testado do mercado para esse tipo de controle de acesso.

### Backup
- Backup automático diário do Postgres (incluso no plano da VPS) + `pg_dump` agendado como segunda camada — **implementado** (Fase 5, `backup_saw_hub_postgres.sh`, cron diário, retenção 30 dias, restauração testada com sucesso).
- **Pendência real:** hoje essa segunda camada só existe *dentro* da própria VPS (`/root/backups/`) — ainda não é armazenada fora do servidor (não adianta backup que morre junto com a VPS). Destino off-site (Backblaze B2 ou outro servidor da Onda) é decisão que falta tomar.

### Criptografia
- **Em trânsito:** TLS em toda comunicação (front↔back↔cliente), via Coolify/Let's Encrypt — automático, renovação sem intervenção manual.
- **Senhas:** nunca armazenadas em texto puro — hash via Spring Security. Fase 5: migrado de BCrypt puro para **Argon2id como padrão pra hashes novos** (`DelegatingPasswordEncoder`, recomendação atual do OWASP Password Storage Cheat Sheet), mantendo compatibilidade com hashes BCrypt já existentes no banco — migração transparente conforme cada usuário troca de senha, sem forçar re-cadastro.
- **Dados sensíveis em repouso** (financeiro do mentorado, dados pessoais): criptografia a nível de coluna no Postgres (`pgcrypto`) — **implementada e verificada em produção** (migration `V19`, colunas armazenadas como `bytea` ilegível, aplicação decifra corretamente via `app.encryption_key`).
- **Criptografia de disco da própria VPS:** **não implementada ainda** — o disco raiz atual é `ext4` puro, sem LUKS. Não dá pra ativar full-disk encryption a quente num servidor já em produção sem reprovisionar (risco real de downtime/perda de dados); fica como item pra uma janela de manutenção planejada, não uma ação de agora. Até lá, a proteção de dados sensíveis em repouso depende só do `pgcrypto` (uma camada, não duas).
- Vale comunicar isso explicitamente ao cliente antes de avançar para não deixar a dúvida em aberto — inclusive a pendência da criptografia de disco, pra não prometer algo que ainda não existe.

## Perfil de projeto
SaaS multi-tenant (1 tenant = 1 mentorado/restaurante) · perfis: **Mentorado** e **Admin (SAW)** · venda por produto/contrato de mentoria, não assinatura recorrente por tier (ver "Produtos & Tipo de Contrato" abaixo).

## Identidade visual
Fornecida pelo cliente (não é a marca da Onda). Base: mockups do SAW HUB — tema **dark**, vinho/bordô + dourado/champagne. Fonte display condensada + sans para UI. Fonte única da verdade: `./design/tokens.css` + `./design/DESIGN.md` (gerados na Fase 2 a partir dos mockups).

## Princípios (não-funcionais críticos)
- **Isolamento por tenant** decidido na modelagem: todo dado de mentorado é escopado por conta. Nunca vazar dados entre mentorados.
- **Auth e Pagamento são os módulos de maior risco** — entram primeiro na esteira e passam por `revisor-seguranca` obrigatório.
- **Responsivo:** **fora do MVP** — cliente confirmou em reunião (07/07/2026) que o MVP não precisa funcionar em mobile; foco é desktop primeiro. Não fechar portas: evitar CSS que assuma só desktop (ex.: larguras fixas absurdas), mas não gastar esforço em mobile agora. Mobile volta como requisito pós-MVP.
- **Escala:** 10–15 usuários esperados no MVP, mas a arquitetura (Postgres + Spring Boot stateless) já nasce pensada pra escalar além disso sem reescrita — não é otimização prematura, é não criar dívida óbvia.
- **Acessibilidade AA** mesmo no tema dark (contraste do dourado sobre vinho verificado).
- **Estados de carregamento e erro** tratados em toda tela que consome dados.
- Papéis e permissões: Mentorado nunca acessa rotas de Admin; Admin não age “como” mentorado sem trilha.

## MVP · Prioridade de construção (definida em reunião com o cliente, 07/07/2026)
O cliente foi claro: **este sistema é a espinha dorsal operacional da SAW**, não só um produto pro mentorado. Prioridade do MVP, nessa ordem:
1. **E1 · Autenticação** — pré-requisito de tudo, constrói primeiro independente da prioridade abaixo.
2. **Núcleo do back-office**: **E13 · Comercial**, **E14 · Financeiro & DRE**, **E15 · Gestão de Time** (RBAC por área) — isso é o foco real do cliente, não os módulos do mentorado.
3. **E17 · Painel Consolidado & Ranking** — depende de mentorados/tarefas existirem, mas pode rodar sobre dados seed enquanto o fluxo completo do mentorado não está pronto.
4. Módulos do mentorado (E2–E9) entram depois, na ordem que já fazia sentido no núcleo (Dashboard → Metas/Tarefas → Mentorias → resto).
- **Objetivo do momento:** ter uma versão apresentável pro cliente o mais rápido possível — prioriza os módulos do item 2 acima com profundidade suficiente pra demonstrar valor, não necessariamente 100% das histórias de cada épico de uma vez.
- Ver "Diferenciais do MVP" abaixo para o que deve entrar já nesta primeira leva para gerar impacto na apresentação.

## Diferenciais do MVP
Cliente pediu explicitamente algo que "feche o projeto na hora". **Confirmado para esta primeira leva** (07/07/2026):
- **Transcrição + rascunho de ata automático via IA** a partir do áudio da mentoria (E5) — o mentor sobe o áudio, o sistema transcreve e gera um rascunho de ata (resumo, encaminhamentos sugeridos) pra revisão humana antes de publicar. Resolve uma dor real (escrever ata manualmente) e é coerente com o resto do produto usar IA.
- Implica: endpoint de upload de áudio, integração com API de transcrição (ex.: Whisper), custo por uso a considerar no orçamento de infra.

## Épicos
> Histórias de usuário completas e critérios de aceite BDD em `./docs/spec.md`.

### Área do Mentorado
1. **E1 · Autenticação & Acesso** *(Grande · risco alto)* — login e-mail/senha, Google OAuth, solicitar acesso, recuperar senha, sessão, perfis.
2. **E2 · Dashboard do Mentorado** *(Médio)* — visão geral: próxima reunião, meta semanal, tarefas abertas, evolução, compromissos, avisos, dica.
3. **E3 · Metas Estratégicas** *(Médio)* — metas com progresso, prazo, status (No prazo/Atenção/Atrasada) e resumo.
4. **E4 · Tarefas & Agenda** *(Médio)* — tarefas por encontro (encaminhamentos), lista/kanban, calendário, filtros, prioridade, vínculo a metas, **peso (1 ou 2) usado no ranking do E17**.
5. **E5 · Mentorias & Atas** *(Médio)* — agenda, histórico, ata por mentoria (**individual e coletiva**, ver E11 para criação em grupo), link Google Meet, materiais recomendados, **upload do áudio gravado na mentoria** vinculado à ata.
6. **E6 · Materiais & Dicas do Brayan** *(Médio)* — biblioteca multi-formato + vídeos, categorias, favoritos, indicadores de consumo.
   - **Backlog (pós-MVP, não implementado):** cliente quer futuramente **upload de vídeo** próprio (não só link externo). Hoje `conteudo.url` é só link (ex.: YouTube) — vídeo autohospedado é um problema diferente do áudio de mentoria (E5): arquivos ordens de magnitude maiores, e precisam ser **transmitidos pra vários mentorados assistirem**, ao contrário do áudio (guardado só pra transcrição, nunca servido). Servir isso direto do disco da VPS arrisca lotar disco e derrubar performance do resto do sistema conforme a biblioteca cresce. Opções levantadas, nenhuma decidida: (a) serviço de streaming de vídeo dedicado (Bunny Stream/Cloudflare Stream — transcodifica e faz streaming adaptativo, custo por uso); (b) object storage genérico (Backblaze B2/Cloudflare R2 — mais barato, sem transcodificação, mesma conta poderia servir também o backup off-site do Postgres já pendente); (c) manter no disco da VPS (só viável se volume esperado for pequeno). Decidir e desenhar antes de começar a implementar.
7. **E7 · Eventos & Inscrições** *(Médio)* — eventos ao vivo/presencial, inscrição, calendário.
8. **E8 · Loja SAW** *(Grande · risco alto)* — catálogo, carrinho, checkout, gateway de pagamento, pedidos.
9. **E9 · Perfil & Gamificação** *(Médio)* — perfil, jornada/nível, XP, conquistas, preferências, tipo de contrato.

### Área Admin (SAW)
10. **E10 · Painel Administrativo & Métricas** *(Médio)* — mentorados ativos, mentorias/eventos realizados, receita, crescimento, distribuição por tipo de contrato, atividades.
    - Inclui **painel consolidado de todos os mentorados** (E17): visão simultânea do "em que pé está cada um", não só o dashboard individual do E2.
11. **E11 · Gestão Admin** *(Grande)* — CRUD de mentorados por tipo de contrato, criação de mentorias (individual/grupo), curadoria de conteúdos e gestão de eventos.

### Gestão interna da SAW (back-office) — solicitado pelo cliente
> Área Admin ampliada para a SAW gerir o próprio negócio (não é ERP do restaurante do mentorado).
13. **E13 · Comercial & Vendas** *(Grande)* — dashboard comercial: leads (solicitações de acesso), funil/pipeline, taxa de conversão, vendas por produto, MRR, vendas da loja, metas e ranking do time comercial.
14. **E14 · Financeiro & DRE** *(Grande · risco alto)* — lançamento de receitas/despesas por categoria, contas a pagar/receber, fluxo de caixa, **DRE** por período e **dashboard de faturamento** (recorrência por tipo de contrato, loja, eventos; MRR/churn; margem/lucro).
15. **E15 · Gestão de Time (SAW)** *(Médio · risco alto pelo escopo de acesso)* — cadastro da equipe interna, carteira de clientes por mentor, metas e desempenho por colaborador. **Acesso por área** (RBAC), não é papel genérico:
    - **Comercial** → só o painel Comercial (E13).
    - **Marketing** → só conteúdos/marketing *(tela dedicada ainda não existe no protótipo — validar com o cliente se reaproveita "Conteúdos" ou é nova, ver Suposições em `spec.md`)*.
    - **Gestão de Performance** → só Mentorados, Mentorias, Conteúdos e o Painel Consolidado (E17). É o papel que hoje o mentorado NÃO acessa (ver Suposições).
    - **Fundador** (cliente e sócio) → acesso irrestrito a tudo, incluindo Financeiro e Time.
17. **E17 · Painel Consolidado de Mentorados & Ranking** *(Grande · risco médio)* — visão geral do progresso de todos os mentorados ao mesmo tempo (não individual); ranking por % de crescimento de faturamento e cumprimento de ferramentas obrigatórias (ficha técnica, DRE, manual da cultura etc.), ponderado pelo peso do encaminhamento (E4).

### Transversal
16. **E16 · Avisos & Notificações** *(Pequeno)* — avisos importantes, convites e notificações in-app/e-mail.

## Máquinas de estado principais
- **Mentoria:** `Agendada → Confirmada → Realizada` (gera ata) · desvio: `→ Cancelada`.
- **Tarefa:** `Pendente → Em andamento → Concluída` · `Atrasada` quando prazo vence sem conclusão.
- **Meta:** `Ativa {No prazo | Atenção | Atrasada} → Concluída` · desvio: `→ Pausada`.
- **Pedido (Loja):** `Carrinho → Aguardando pagamento → Pago → Liberado` · desvios: `Cancelado`, `Reembolsado`.
- **Inscrição em evento:** `Disponível → Inscrito → Participado` · desvio: `Cancelada`.
- **Lead comercial:** `Solicitação → Em contato → Proposta → Fechado` · desvio: `Perdido`.
- **Lançamento financeiro:** `Previsto → Realizado` · conta: `A pagar/A receber → Pago/Recebido` (ou `Vencido`).

## Produtos & Tipo de Contrato
Decisão do cliente (reunião 17/07/2026): **"não existem planos, mas sim produtos"** — o antigo `Plano` (Gratuito/Básico/Essencial/Profissional, com cobrança recorrente por tier) não reflete o modelo de negócio real e foi **removido por completo do sistema** (schema, backend, frontend — M28, concluído 21/07/2026).
- **`TipoContrato`** (aditivo em `Mentorado`, nullable) é o que define o contrato de mentoria de um mentorado: **Mentoria Contínua** (12 meses, sessão semanal em grupo + 2 individuais/ano com o Mateus, 3 eventos grátis/ano) · **Mentoria Individual** (12 meses, sessão mensal individual em vez das 2/ano) · **Consultoria** (sem prazo fixo/"esporádica", sessão semanal individual, sem acesso ao conteúdo da mentoria contínua em grupo).
- **`ProdutoVenda`** (Comercial/E13) é o catálogo mais amplo do que a SAW efetivamente vende — inclui os 3 tipos acima e também `FORMULA_SAW`, `FORMACAO_PROFISSIONAL`, `FICHA_TECNICA_LUCRATIVA`, `INGRESSO_EVENTO`, `PRODUTO_DIGITAL`. Só os 3 primeiros propagam pra `TipoContrato` no Mentorado (os demais não são contrato de mentoria).
- Sem cobrança recorrente por tier — venda é por produto/contrato fechado (ver máquina de estado "Lead comercial" abaixo).

## Convenções
- API REST `/api/v1`, JSON, erros padronizados.
- Diretiva Primária na Fase 4: não alterar sintaxe de código existente.
- Datas em `pt-BR`; moeda em `R$`.
- Responsivo mobile fica fora do MVP (ver Princípios) — não é mais requisito de toda tela por enquanto.

## Ponteiros
- Histórias completas + BDD: `./docs/spec.md`
- Blueprint técnico: `./ROADMAP.md` (Fase 3)
- Identidade visual: `./design/tokens.css` + `./design/DESIGN.md` (Fase 2)
- Referência visual do cliente: `onda-propostas/clientes/saw/SAW HUB.pdf` (15 telas)
- **`design/prototipo/` está congelado** — protótipo estático da Fase 2b, não editar mais. Referência visual para a Fase 4, não o código de produção.
- **Código real do MVP:** `frontend/` (React+Vite) e `backend/` (Spring Boot) na raiz do repositório, a partir da Fase 4.
- **Dev diário:** `./scripts/dev-up.sh` / `./scripts/dev-down.sh` — sobe infra (Docker) + backend + frontend nativos (hot-reload real). Ver `docs/DEV.md`.
- **Testes E2E:** `./scripts/e2e-up.sh` / `./scripts/e2e-down.sh` — stack isolada (banco, Redis e portas próprias) só pra rodar o Playwright. **Nunca** rodar a suíte E2E contra o `dev-up.sh` — os specs criam dados reais sem teardown e poluem o ambiente que você está navegando/demonstrando. Ver `docs/DEV.md`.
- **Dockerfiles de produção** (`backend/Dockerfile`, `frontend/Dockerfile` + `nginx.conf`) prontos pro build do Coolify. `docker-compose.full.yml` sobe a stack inteira containerizada pra testar antes do deploy (`./scripts/full-up.sh` / `./scripts/full-down.sh`) — não é o fluxo de dev diário, só sanity-check pré-deploy.
- **Transição de acessos pro cliente** (Google/Mercado Pago/VPS/GitHub/e-mail/IA — hoje em contas pessoais/Onda): plano detalhado em `./transicao-acessos-para-cliente.md` (arquivo local, não versionado — contém dado de conta).
- **Custos operacionais mapeados** (hospedagem, domínio, IA — Whisper/Claude, taxa Mercado Pago, e-mail): `./custos-operacionais.md` (arquivo local, não versionado). Preços de mercado pesquisados em 16/07/2026, reconfirmar na fonte antes de repassar como número fechado.
