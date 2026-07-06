# SAW HUB — Briefing para o Claude Design

> Cole este texto no **claude.ai/design** para gerar o protótipo navegável. Ele carrega a identidade
> da SAW (tokens reais dos mockups) + as 15 telas. Depois use **"Send to Claude Code Web"** para
> trazer de volta ao projeto `saw-hub`.

---

## Contexto
Gere um **protótipo navegável de alta fidelidade** (dark mode) de um **SaaS de mentoria para donos de
restaurante** chamado **SAW HUB**. Duas áreas: **Mentorado** e **Admin (SAW)**. Dados fictícios em
pt-BR. Responsivo (desktop + mobile). Acessível (AA). Sidebar clicável navegando entre as telas.

## Identidade visual (obrigatória — não inventar cores)
Tema **dark**, premium e sóbrio (vinho + dourado sobre near-black).

- Dourado (ação primária, logo, destaques): `#F0B050` · hover `#F8C85E` · dourado suave `#D9BE93`
- Vinho/bordô (marca, login, faixas): `#57191C` · profundo `#3C1013`
- Fundo do app: `#0C0C0C` · cartão/painel: `#141414` · superfície 2: `#1B1917` · borda: `#2A2724`
- Texto: `#F4EEE4` (creme) · secundário `#A9A29A` · terciário `#6E6862`
- Sobre botão dourado, texto quase-preto `#1A1206`
- Status: verde `#3FB27F` (no prazo/concluído/pago) · âmbar `#F0B050` (atenção/pendente) ·
  vermelho `#E5573F` (atrasado/ao vivo/cancelado) · azul `#5AA9E6` (visita/info)
- Cantos: cartões 16px, botões/chips pílula. Sombra escura difusa para baixo. Foco dourado translúcido.
- Tipografia: **Inter** para toda a UI; **Anton** (condensado, MAIÚSCULAS) só no wordmark "SAW" e no hero de login.

## Componentes-base
Sidebar 248px (logo SAW no topo, itens com ícone linear, item ativo com realce dourado) · topbar com
saudação + sino de notificações + avatar/plano · cartões `#141414` · KPI (rótulo + número grande +
delta colorido) · chips de status (fundo translúcido + ícone + rótulo, nunca só cor) · tabelas
(cabeçalho discreto uppercase, linhas com divisor sutil) · barra de progresso dourada · player de
vídeo com play dourado · gráficos (linha de evolução, rosca de distribuição).

## Dados fictícios (use estes)
Mentorado: **João Silva**, "Restaurante Sabor & Arte", plano **Profissional**, nível **Ouro**.
Mentor/fundador: **Matheus Brayan**. Metas típicas: "CMV abaixo de 30%", "Aumentar ticket médio".

---

## Telas — Área do Mentorado (10)
1. **Login** — card escuro com logo SAW dourado, campos e-mail/senha, "Entrar" (dourado), "Entrar com Google", "Solicitar acesso", "Esqueci a senha". Painel lateral vinho com foto do Matheus e citação.
2. **Dashboard** — saudação "Bom dia, João!"; 4 KPIs (Próxima reunião 28/06, Meta semanal 75%, Tarefas abertas 4, Evolução geral 82%); gráfico "Seu progresso" (linha, últimos 6 meses); "Próximos compromissos" (mentoria/visita/evento/tarefa com chips); "Avisos importantes"; "Dica do Brayan" (vídeo em destaque).
3. **Metas estratégicas** — abas Ativas/Concluídas/Pausadas/Todas; tabela de metas com progresso (%), prazo, dias restantes, status (No prazo/Atenção/Atrasada), ação; linha de resumo geral.
4. **Tarefas** — 4 KPIs (Total 24, Concluídas 14, Em andamento 7, Pendentes 3); abas + filtro + busca; tabela (tarefa, meta relacionada, prazo, status, prioridade) com checkbox; calendário lateral e "Próximas tarefas".
5. **Mentorias & ata** — "Próxima mentoria" com "Entrar na reunião" (Google Meet); histórico com status; painel de detalhes (data, hora, local, mentor) + materiais da mentoria; "Adicionar ao calendário".
6. **Materiais** — abas por categoria (Planilhas/Templates/Apresentações/Documentos/Vídeos/E-books/Áudios); busca; tabela (material, categoria, formato, data, ações) + painel de detalhe do item selecionado com "Baixar material".
7. **Dicas do Brayan** — grade de vídeos com thumb+play, duração, visualizações, categoria; "Destaque da semana"; categorias com contagem; "Seus indicadores" (dias assistidos, minutos, favoritas).
8. **Eventos** — 4 KPIs; abas Todos/Próximos/Ao vivo/Realizados; lista de eventos (ao vivo/presencial/workshop/webinar) com data, local/online, participantes, "Inscrever-se"; calendário e "Próximos eventos".
9. **Loja SAW** — barra de categorias (Cursos/Planilhas/Templates/E-books/Ferramentas/Kits/Consultorias); "Destaques" e "Mais vendidos" (cards com preço, avaliação, carrinho); carrinho lateral com subtotal e "Finalizar compra"; selos de confiança.
10. **Perfil** — cabeçalho com avatar, nome, restaurante, contato, áreas de interesse; abas Visão geral/Preferências/Segurança/Notificações/Integrações; "Minha jornada SAW" (nível Ouro, XP, barra); conquistas; "Resumo da conta" (plano, vencimento) e assinatura.

## Telas — Área Admin / SAW (5)
11. **Dashboard administrativo** — KPIs (Mentorados ativos 1.248, Mentorias realizadas 243, Eventos 24, Receita do mês R$ 24.870) com variação; gráfico de crescimento de mentorados; rosca de distribuição por plano (Gratuito/Básico/Essencial/Profissional); atividades recentes; mentorias agendadas para hoje.
12. **Mentorados** — tabela paginada (mentorado, plano, status Ativo/Inativo, cadastro, ações ver/editar/remover); busca + filtros por plano/status; "Exportar" e "Novo mentorado".
13. **Nova mentoria** — formulário: tipo (Individual/Grupo), mentorado(s), título, descrição/pauta; detalhes (data, hora, duração, plataforma Google Meet, enviar convite por e-mail); "Criar mentoria".
14. **Gestão de eventos** — tabela (evento, tipo, data, participantes, status Programado/Ao vivo/Realizado/Cancelado, ações); abas + filtros; "Novo evento".
15. **Biblioteca de conteúdos** — tabela (conteúdo, tipo, categoria, acessos, data, status Publicado/Rascunho, ações); abas por tipo; "Exportar" e "Novo conteúdo".

## Instruções finais
- Sidebar do Mentorado: Dashboard, Mentorias, Metas, Tarefas, Materiais, Dicas do Brayan, Eventos, Loja SAW, Avisos, Perfil, Suporte.
- Sidebar do Admin: Dashboard, Mentorados, Mentorias, Eventos, Conteúdos, Relatórios, Financeiro, Configurações, Suporte.
- Navegação clicável entre todas as telas. Mantenha consistência absoluta de espaçamento, cores e componentes entre as 15 telas.
