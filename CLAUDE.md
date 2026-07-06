# SAW HUB

Plataforma SaaS de mentoria para restaurantes: centraliza a jornada do mentorado (agenda, metas, tarefas, atas, conteúdos, eventos e loja) e dá à SAW um painel de operação e métricas.

## Stack
- **Frontend:** Next.js (App Router) + React · TypeScript
- **Backend:** Node.js + Fastify + Prisma
- **Banco:** PostgreSQL (relacional) + Redis (cache, filas, sessão)
- **Auth:** Auth.js (e-mail/senha + Google OAuth) · perfis Mentorado e Admin
- **Fila:** BullMQ + Redis (e-mails, lembretes de mentoria)
- **Pagamento:** gateway externo (Mercado Pago / Stripe / Pagar.me) na Loja
- **Deploy:** Vercel (front) + Railway/Render (API/banco)
- **Protótipo (Fase 2b):** front 100% estático com dados fictícios, componentizado, para aprovação do cliente — sem backend.

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
4. **E4 · Tarefas & Agenda** *(Médio)* — tarefas por encontro, lista/kanban, calendário, filtros, prioridade, vínculo a metas.
5. **E5 · Mentorias & Atas** *(Médio)* — agenda, histórico, ata por mentoria, link Google Meet, materiais recomendados.
6. **E6 · Materiais & Dicas do Brayan** *(Médio)* — biblioteca multi-formato + vídeos, categorias, favoritos, indicadores de consumo.
7. **E7 · Eventos & Inscrições** *(Médio)* — eventos ao vivo/presencial, inscrição, calendário.
8. **E8 · Loja SAW** *(Grande · risco alto)* — catálogo, carrinho, checkout, gateway de pagamento, pedidos.
9. **E9 · Perfil & Gamificação** *(Médio)* — perfil, jornada/nível, XP, conquistas, preferências, assinatura/plano.

### Área Admin (SAW)
10. **E10 · Painel Administrativo & Métricas** *(Médio)* — mentorados ativos, mentorias/eventos realizados, receita, crescimento, distribuição por plano, atividades.
11. **E11 · Gestão Admin** *(Grande)* — CRUD de mentorados por plano, criação de mentorias (individual/grupo), curadoria de conteúdos e gestão de eventos.

### Transversal
12. **E12 · Avisos & Notificações** *(Pequeno)* — avisos importantes, convites e notificações in-app/e-mail.

## Máquinas de estado principais
- **Mentoria:** `Agendada → Confirmada → Realizada` (gera ata) · desvio: `→ Cancelada`.
- **Tarefa:** `Pendente → Em andamento → Concluída` · `Atrasada` quando prazo vence sem conclusão.
- **Meta:** `Ativa {No prazo | Atenção | Atrasada} → Concluída` · desvio: `→ Pausada`.
- **Pedido (Loja):** `Carrinho → Aguardando pagamento → Pago → Liberado` · desvios: `Cancelado`, `Reembolsado`.
- **Inscrição em evento:** `Disponível → Inscrito → Participado` · desvio: `Cancelada`.

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
