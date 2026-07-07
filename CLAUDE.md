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

## Segurança e persistência dos dados (dúvida recorrente do cliente)
O cliente comparou o projeto com ferramentas no-code (ex.: Base44) e perguntou sobre risco de perder dados.
Resposta de referência para reforçar com ele:
- O que ele viu até agora (`design/prototipo/`) é **só a fachada visual da Fase 2** — dados 100% fictícios, sem banco, sem persistência. Ainda não há nada "real" para perder.
- O sistema de verdade (Fase 4) usa **Postgres em VPS própria** (Hostinger, com backup automático) — a SAW é dona do banco, do código e do servidor, não depende da sobrevivência de uma ferramenta no-code de terceiros.
- **Java + Spring Security** é a escolha para o backend justamente pela maturidade em regras de permissão complexas (RBAC por área no E15) — não é só uma preferência de conforto, é o ecossistema mais testado do mercado para esse tipo de controle de acesso.
- Vale comunicar isso explicitamente antes de avançar para não deixar a dúvida em aberto.

## Perfil de projeto
SaaS multi-tenant (1 tenant = 1 mentorado/restaurante) · perfis: **Mentorado** e **Admin (SAW)** · produto recorrente por assinatura (planos).

## Identidade visual
Fornecida pelo cliente (não é a marca da Onda). Base: mockups do SAW HUB — tema **dark**, vinho/bordô + dourado/champagne. Fonte display condensada + sans para UI. Fonte única da verdade: `./design/tokens.css` + `./design/DESIGN.md` (gerados na Fase 2 a partir dos mockups).

## Princípios (não-funcionais críticos)
- **Isolamento por tenant** decidido na modelagem: todo dado de mentorado é escopado por conta. Nunca vazar dados entre mentorados.
- **Auth e Pagamento são os módulos de maior risco** — entram primeiro na esteira e passam por `revisor-seguranca` obrigatório.
- **Responsivo** de verdade: desktop e mobile (o cliente exige acesso por computador e celular).
- **Acessibilidade AA** mesmo no tema dark (contraste do dourado sobre vinho verificado).
- **Estados de carregamento e erro** tratados em toda tela que consome dados.
- Papéis e permissões: Mentorado nunca acessa rotas de Admin; Admin não age “como” mentorado sem trilha.

## Épicos
> Histórias de usuário completas e critérios de aceite BDD em `./docs/spec.md`.

### Área do Mentorado
1. **E1 · Autenticação & Acesso** *(Grande · risco alto)* — login e-mail/senha, Google OAuth, solicitar acesso, recuperar senha, sessão, perfis.
2. **E2 · Dashboard do Mentorado** *(Médio)* — visão geral: próxima reunião, meta semanal, tarefas abertas, evolução, compromissos, avisos, dica.
3. **E3 · Metas Estratégicas** *(Médio)* — metas com progresso, prazo, status (No prazo/Atenção/Atrasada) e resumo.
4. **E4 · Tarefas & Agenda** *(Médio)* — tarefas por encontro (encaminhamentos), lista/kanban, calendário, filtros, prioridade, vínculo a metas, **peso (1 ou 2) usado no ranking do E17**.
5. **E5 · Mentorias & Atas** *(Médio)* — agenda, histórico, ata por mentoria, link Google Meet, materiais recomendados.
6. **E6 · Materiais & Dicas do Brayan** *(Médio)* — biblioteca multi-formato + vídeos, categorias, favoritos, indicadores de consumo.
7. **E7 · Eventos & Inscrições** *(Médio)* — eventos ao vivo/presencial, inscrição, calendário.
8. **E8 · Loja SAW** *(Grande · risco alto)* — catálogo, carrinho, checkout, gateway de pagamento, pedidos.
9. **E9 · Perfil & Gamificação** *(Médio)* — perfil, jornada/nível, XP, conquistas, preferências, assinatura/plano.

### Área Admin (SAW)
10. **E10 · Painel Administrativo & Métricas** *(Médio)* — mentorados ativos, mentorias/eventos realizados, receita, crescimento, distribuição por plano, atividades.
    - Inclui **painel consolidado de todos os mentorados** (E17): visão simultânea do "em que pé está cada um", não só o dashboard individual do E2.
11. **E11 · Gestão Admin** *(Grande)* — CRUD de mentorados por plano, criação de mentorias (individual/grupo), curadoria de conteúdos e gestão de eventos.

### Gestão interna da SAW (back-office) — solicitado pelo cliente
> Área Admin ampliada para a SAW gerir o próprio negócio (não é ERP do restaurante do mentorado).
13. **E13 · Comercial & Vendas** *(Grande)* — dashboard comercial: leads (solicitações de acesso), funil/pipeline, taxa de conversão, vendas por plano, MRR, vendas da loja, metas e ranking do time comercial.
14. **E14 · Financeiro & DRE** *(Grande · risco alto)* — lançamento de receitas/despesas por categoria, contas a pagar/receber, fluxo de caixa, **DRE** por período e **dashboard de faturamento** (recorrência por plano, loja, eventos; MRR/churn; margem/lucro).
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

## Planos
`Gratuito · Básico · Essencial · Profissional` — controlam acesso a conteúdos, nº de mentorias e recursos. Cobrança recorrente (assinatura).

## Convenções
- API REST `/api/v1`, JSON, erros padronizados.
- Diretiva Primária na Fase 4: não alterar sintaxe de código existente.
- Datas em `pt-BR`; moeda em `R$`.
- Toda tela existe em desktop e mobile.

## Ponteiros
- Histórias completas + BDD: `./docs/spec.md`
- Blueprint técnico: `./ROADMAP.md` (Fase 3)
- Identidade visual: `./design/tokens.css` + `./design/DESIGN.md` (Fase 2)
- Referência visual do cliente: `onda-propostas/clientes/saw/SAW HUB.pdf` (15 telas)
