# SAW HUB · Especificação Viva (spec.md)

> Fonte única das histórias de usuário e critérios de aceite (BDD). Referência visual: as 15 telas
> em `onda-propostas/clientes/saw/SAW HUB.pdf`. Perfis: **Mentorado** e **Admin (SAW)**.

---

## E1 · Autenticação & Acesso  *(Grande · risco alto)*

**H1.1** — Como mentorado, quero entrar com e-mail e senha para acessar minha jornada.
- **Dado** que tenho conta ativa, **quando** informo e-mail e senha corretos, **então** sou levado ao meu Dashboard.
- **Dado** credenciais inválidas, **quando** tento entrar, **então** vejo mensagem de erro clara sem revelar qual campo falhou.

**H1.2** — Como mentorado, quero entrar com o Google para acessar sem criar senha.
- **Dado** que clico em "Entrar com Google", **quando** autorizo a conta, **então** entro na plataforma; se for o 1º acesso, minha conta é vinculada.

**H1.3** — Como visitante, quero "Solicitar acesso" para pedir entrada na mentoria.
- **Dado** que não tenho conta, **quando** envio a solicitação, **então** ela chega ao Admin para aprovação e recebo confirmação.

**H1.4** — Como mentorado, quero recuperar minha senha para voltar a acessar.
- **Dado** que esqueci a senha, **quando** peço recuperação, **então** recebo um link temporário por e-mail.

**H1.5** — Como sistema, quero separar perfis para proteger rotas.
- **Dado** um usuário Mentorado, **quando** tenta abrir uma rota de Admin, **então** o acesso é negado.

---

## E2 · Dashboard do Mentorado  *(Médio)*

**H2.1** — Como mentorado, quero uma visão geral ao entrar para saber o que fazer hoje.
- **Dado** que acesso o Dashboard, **então** vejo: próxima reunião, meta semanal (%), tarefas abertas e evolução geral (%).

**H2.2** — Como mentorado, quero ver meus próximos compromissos para me organizar.
- **Dado** que tenho mentorias/visitas/eventos futuros, **quando** abro o Dashboard, **então** vejo a lista ordenada por data com tipo etiquetado.

**H2.3** — Como mentorado, quero ver avisos importantes e a dica do Brayan em destaque.
- **Dado** que há avisos novos, **quando** abro o Dashboard, **então** eles aparecem com data e ação ("Ver todos"), além da dica em vídeo em destaque.

---

## E3 · Metas Estratégicas  *(Médio)*

**H3.1** — Como mentorado, quero acompanhar minhas metas com progresso e prazo.
- **Dado** que tenho metas ativas, **então** vejo cada uma com %, prazo, dias restantes e status (No prazo / Atenção / Atrasada).

**H3.2** — Como mentorado, quero filtrar metas por estado.
- **Dado** os filtros Ativas/Concluídas/Pausadas/Todas, **quando** seleciono um, **então** a lista reflete apenas aquele estado.

**H3.3** — Como mentorado, quero um resumo geral das metas.
- **Dado** minhas metas, **então** vejo total concluído, nº concluídas, no prazo e atrasadas.

---

## E4 · Tarefas & Agenda  *(Médio)*

**H4.1** — Como mentorado, quero ver as tarefas definidas em cada encontro.
- **Dado** que tenho tarefas, **então** vejo tabela com tarefa, meta relacionada, prazo, status e prioridade.

**H4.2** — Como mentorado, quero marcar tarefa como concluída.
- **Dado** uma tarefa pendente, **quando** a concluo, **então** seu status vira "Concluída" e conta no resumo.

**H4.3** — Como mentorado, quero filtrar e buscar tarefas.
- **Dado** filtros (Todas/Pendentes/Em andamento/Concluídas) e busca, **quando** aplico, **então** a lista e o calendário refletem o filtro.

**H4.4** — Como sistema, quero sinalizar atraso.
- **Dado** uma tarefa não concluída, **quando** o prazo vence, **então** ela passa a "Atrasada".

---

## E5 · Mentorias & Atas  *(Médio)*

**H5.1** — Como mentorado, quero ver minha próxima mentoria e entrar na reunião.
- **Dado** uma mentoria agendada online, **quando** chega o horário, **então** o botão "Entrar na reunião" (Google Meet) fica disponível.

**H5.2** — Como mentorado, quero acessar o histórico e a ata de cada mentoria.
- **Dado** mentorias realizadas, **quando** abro uma, **então** vejo a ata, data, local, mentor e materiais recomendados.

**H5.3** — Como mentorado, quero adicionar a mentoria ao meu calendário.
- **Dado** os detalhes da mentoria, **quando** clico "Adicionar ao calendário", **então** recebo o evento (.ics/Google).

---

## E6 · Materiais & Dicas do Brayan  *(Médio)*

**H6.1** — Como mentorado, quero navegar a biblioteca de materiais por categoria e formato.
- **Dado** materiais (XLSX/PPTX/PDF/DOCX/vídeo/áudio), **quando** filtro por categoria/formato, **então** vejo os itens com data e ação de download.

**H6.2** — Como mentorado, quero favoritar materiais e dicas.
- **Dado** um item, **quando** clico em favoritar, **então** ele aparece nos meus favoritos.

**H6.3** — Como mentorado, quero assistir às dicas do Brayan em vídeo.
- **Dado** a lista de dicas, **quando** abro uma, **então** o player toca e o consumo conta nos meus indicadores (dias assistidos, minutos, favoritas).

---

## E7 · Eventos & Inscrições  *(Médio)*

**H7.1** — Como mentorado, quero ver eventos ao vivo e presenciais.
- **Dado** eventos programados, **quando** filtro por tipo/tema, **então** vejo cada um com data, local/online e participantes.

**H7.2** — Como mentorado, quero me inscrever em um evento.
- **Dado** um evento com vagas, **quando** clico "Inscrever-se", **então** minha inscrição é registrada e o evento entra em "Próximos eventos".

**H7.3** — Como mentorado, quero um calendário de eventos.
- **Dado** o calendário, **quando** seleciono um dia, **então** vejo os eventos daquele dia.

---

## E8 · Loja SAW  *(Grande · risco alto)*

**H8.1** — Como mentorado, quero navegar o catálogo por categoria.
- **Dado** produtos (cursos, planilhas, templates, e-books, ferramentas, kits, consultorias), **quando** filtro/busco, **então** vejo destaques e mais vendidos com preço e avaliação.

**H8.2** — Como mentorado, quero adicionar itens ao carrinho.
- **Dado** um produto, **quando** clico no carrinho, **então** ele entra no carrinho e o subtotal atualiza.

**H8.3** — Como mentorado, quero finalizar a compra com pagamento seguro.
- **Dado** itens no carrinho, **quando** finalizo, **então** sou levado ao checkout do gateway; **quando** o pagamento é aprovado, **então** o pedido vira "Pago" e o item digital é liberado.
- **Dado** pagamento recusado, **quando** retorno, **então** vejo o motivo e o carrinho é preservado.

**H8.4** — Como sistema, quero registrar o pedido com trilha.
- **Dado** um checkout, **então** o pedido percorre `Aguardando pagamento → Pago → Liberado` (ou `Cancelado/Reembolsado`), com registro de cada transição.

---

## E9 · Perfil & Gamificação  *(Médio)*

**H9.1** — Como mentorado, quero ver e editar meu perfil.
- **Dado** meu perfil, **quando** edito dados/preferências, **então** as mudanças são salvas.

**H9.2** — Como mentorado, quero ver minha jornada e conquistas.
- **Dado** meu progresso, **então** vejo nível (ex.: Ouro), XP para o próximo nível, materiais/dicas/eventos e conquistas recentes.

**H9.3** — Como mentorado, quero gerenciar minha assinatura.
- **Dado** meu plano atual, **quando** abro "Gerenciar plano", **então** vejo vencimento e opções de upgrade/downgrade.

---

## E10 · Painel Administrativo & Métricas  *(Médio)*

**H10.1** — Como Admin, quero uma visão geral da plataforma.
- **Dado** o Dashboard admin, **então** vejo mentorados ativos, mentorias e eventos realizados e receita do mês, com variação.

**H10.2** — Como Admin, quero ver crescimento e distribuição por plano.
- **Dado** os dados, **então** vejo gráfico de crescimento de mentorados e a distribuição por plano (Gratuito/Básico/Essencial/Profissional).

**H10.3** — Como Admin, quero ver atividades recentes e mentorias do dia.
- **Dado** o Dashboard admin, **então** vejo atividades recentes e as mentorias agendadas para hoje.

---

## E11 · Gestão Admin  *(Grande)*

**H11.1** — Como Admin, quero gerenciar mentorados por plano e status.
- **Dado** a lista de mentorados, **quando** filtro por plano/status e busco, **então** vejo cada mentorado com plano, status e ações (ver/editar/remover).

**H11.2** — Como Admin, quero criar uma nova mentoria (individual ou em grupo).
- **Dado** o formulário, **quando** escolho tipo, mentorado(s), data, hora, duração e plataforma, **então** a mentoria é criada e (opcional) o convite é enviado por e-mail.

**H11.3** — Como Admin, quero gerir a biblioteca de conteúdos.
- **Dado** conteúdos, **quando** crio/edito/publico, **então** eles ficam disponíveis para os mentorados conforme o plano.

**H11.4** — Como Admin, quero gerir os eventos.
- **Dado** eventos, **quando** crio/edito e mudo status (Programado/Ao vivo/Realizado/Cancelado), **então** os mentorados veem o estado correto.

---

## E13 · Comercial & Vendas (SAW)  *(Grande)*

**H13.1** — Como SAW, quero um dashboard comercial para acompanhar as vendas.
- **Dado** o painel comercial, **então** vejo novos mentorados no mês, taxa de conversão, MRR e vendas da loja, com variação.

**H13.2** — Como SAW, quero um funil de vendas para saber onde perco negócios.
- **Dado** leads (solicitações de acesso), **quando** avançam de etapa (Em contato → Proposta → Fechado/Perdido), **então** o funil e a conversão refletem a mudança.

**H13.3** — Como SAW, quero acompanhar as metas e o ranking do time comercial.
- **Dado** metas por vendedor, **então** vejo o realizado x meta e o ranking do time no período.

---

## E14 · Financeiro & DRE (SAW)  *(Grande · risco alto)*

**H14.1** — Como SAW, quero lançar receitas e despesas para controlar o caixa.
- **Dado** a tela de lançamentos, **quando** registro uma receita/despesa (valor, categoria, data, conta), **então** ela entra no fluxo de caixa e nos relatórios.

**H14.2** — Como SAW, quero um DRE por período para ver o resultado do negócio.
- **Dado** receitas e despesas classificadas, **quando** escolho um período, **então** vejo Receita bruta → líquida → custos → despesas → **resultado (lucro/EBITDA)**, com comparativo mês a mês.

**H14.3** — Como SAW, quero um dashboard de faturamento.
- **Dado** as fontes de receita (assinaturas por plano, loja, eventos), **então** vejo faturamento mensal, MRR, churn e composição da receita.

**H14.4** — Como SAW, quero contas a pagar/receber com status.
- **Dado** uma conta, **quando** o pagamento é feito, **então** ela passa de `A pagar/A receber` para `Pago/Recebido`; vencidas são sinalizadas.

---

## E15 · Gestão de Time (SAW)  *(Médio)*

**H15.1** — Como SAW, quero cadastrar a equipe interna com papéis e permissões.
- **Dado** um colaborador (mentor/comercial/atendimento), **quando** defino o papel, **então** ele acessa apenas as áreas permitidas.

**H15.2** — Como SAW, quero ver a carteira de clientes por mentor.
- **Dado** os mentores, **então** vejo quantos mentorados cada um atende e a distribuição da carteira.

**H15.3** — Como SAW, quero acompanhar metas e desempenho do time.
- **Dado** metas por colaborador, **então** vejo mentorias realizadas, conversões e realizado x meta no período.

---

## E16 · Avisos & Notificações  *(Pequeno · transversal)*

**H12.1** — Como mentorado, quero receber avisos e convites importantes.
- **Dado** um aviso publicado pelo Admin, **quando** acesso a plataforma, **então** ele aparece no sino de notificações e na seção de avisos.

**H12.2** — Como Admin, quero publicar avisos e convites.
- **Dado** um aviso, **quando** publico, **então** os mentorados-alvo passam a vê-lo.

---

## Suposições a validar com a SAW
1. **Planos e o que cada um libera** (conteúdos, nº de mentorias, loja) — os nomes vieram dos mockups; falta a regra de cada plano.
2. **Gateway de pagamento** a ser usado (Mercado Pago / Stripe / Pagar.me) e emissão fiscal.
3. **Mentoria em grupo** — quantos mentorados por grupo e como a ata é compartilhada.
4. **Origem dos vídeos** (upload próprio vs. YouTube/Vimeo não listado) das dicas e materiais.
5. **Integração de agenda** — apenas Google Meet ou também Zoom/presencial com endereço.
6. **App nativo** fica fora do MVP (web responsiva atende); confirmar.
