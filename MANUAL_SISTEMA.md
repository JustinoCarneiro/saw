# SAW HUB — Manual do Sistema

Bem-vindo ao **SAW HUB**! Este manual apresenta uma visão geral de todas as funcionalidades disponíveis na plataforma, desenvolvida para centralizar a gestão da Escola de Restaurantes. O sistema é dividido em módulos específicos para cada área do seu negócio.

---

## 1. Acesso ao Sistema
A plataforma possui dois tipos principais de acesso:
- **Painel Administrativo:** Acessado por você (Fundador) e sua equipe (Colaboradores). O que cada pessoa da equipe enxerga depende da sua **Área** (Comercial, Marketing, Gestão de Performance). O Fundador (Admin Geral) enxerga tudo.
- **Painel do Mentorado:** Acessado pelos clientes. Eles têm uma visão simplificada e focada no desenvolvimento deles (acesso aos materiais, agenda de mentorias, plano de ação e loja).

**Como fazer o acesso manual:**
No centro da tela de login, preencha o campo **E-mail** (texto) e **Senha** (texto). Clique no botão principal amarelo **"Entrar"**.

**Dica de Apresentação:** Para acesso rápido em demonstrações, adicione `?demo=1` ao final da URL na tela de login (ex: `http://localhost:5173/login?demo=1`). Aparecerá a seção **Acesso rápido (Demo)** logo abaixo do botão "Solicitar acesso". Clique no botão do perfil desejado (ex: "Preencher Admin") e os campos serão automaticamente preenchidos. Depois, basta clicar em "Entrar".

---

## 2. Benefícios e Fluxos por Perfil de Usuário

O SAW HUB não é apenas um sistema de controle, mas uma ferramenta de aceleração para todos os envolvidos. Cada perfil tem uma jornada pensada para maximizar seus resultados:

### 👑 Fundador / Direção (Admin Geral)
- **Fluxo Principal:** Acompanhamento Executivo.
- **Benefícios:** Visão 360º de todas as áreas (Vendas, Finanças, e Entregas). O fundador não precisa pedir relatórios: o Dashboard atualiza em tempo real o fluxo de caixa, a margem de lucro (DRE), as assinaturas ativas e o desempenho de cada funcionário.

### 💼 Consultor Comercial (Equipe de Vendas)
- **Fluxo Principal:** Prospecção, Negociação e Fechamento.
- **Benefícios:** Elimina o uso de planilhas desorganizadas. O Kanban visual (funil de vendas) impede que negociações "esfriem". O consultor visualiza sua meta diária/mensal batendo na tela, mantendo o foco em converter leads em clientes reais.

### 🚀 Mentor / Gestor de Performance (Equipe Técnica)
- **Fluxo Principal:** Realização de Mentorias e Acompanhamento.
- **Benefícios:** Automação do "trabalho braçal". Em vez de perder horas escrevendo relatórios após as reuniões, a **Inteligência Artificial** gera a ata automaticamente e sugere tarefas. O mentor consegue atender o dobro de clientes mantendo um relacionamento de alta qualidade.

### 🎓 Mentorado (O Cliente)
- **Fluxo Principal:** Evolução do Negócio, Consumo de Materiais e Acompanhamento de Metas.
- **Benefícios:** Fim da desorganização (links no WhatsApp, arquivos perdidos no e-mail). O mentorado acessa um **portal premium** onde tem todo o seu plano de ação gamificado (barra de progresso), sua agenda de sessões unificada, biblioteca de conteúdos e acesso direto à Loja para comprar extras. Tudo isso gera uma percepção de muito mais autoridade e organização da Escola.

---

## 3. Dashboard Consolidado (Visão Geral)
Logo ao entrar com permissão total, você acessa o Dashboard Consolidado, que reúne os indicadores vitais do negócio em tempo real:
- **Saúde Financeira:** MRR (Receita Recorrente Mensal), taxa de Churn (cancelamentos), ticket médio e inadimplência.
- **Funil Comercial:** Quantidade de Leads ativos em cada etapa da negociação e taxa de conversão do mês.
- **Engajamento Operacional:** Total de mentorias realizadas no mês e nível de engajamento médio dos mentorados com a plataforma.

---

## 4. Módulo Comercial (Vendas)
O coração da sua captação de clientes.
- **Gestão de Leads:** Interface em estilo Kanban (colunas) para você arrastar e acompanhar os leads desde o primeiro contato até o fechamento ou perda da negociação. Para mover um lead de etapa, clique no cartão dele, segure e arraste para a coluna ao lado.
- **Como adicionar um Lead:** Clique no botão **"+ Criar Lead"** no canto superior direito da tela. Uma janela será aberta. Preencha o **Nome**, **E-mail** e o **Plano de Interesse** (selecione na lista suspensa). Clique em **Salvar**. Ele aparecerá na primeira coluna ("Solicitação").
- **Métricas Individuais:** Consultores de vendas podem ver a própria meta do mês, quantos fechamentos realizaram e a porcentagem de batimento da meta na lateral ou topo da tela.

---

## 5. Módulo Financeiro
Controle total do caixa da empresa sem precisar de planilhas externas.
- **Contas a Pagar e Receber:** Acompanhe o que está pendente.
  - *Passo a passo:* Para adicionar uma conta, clique em **"+ Nova Conta"** no canto superior direito. Preencha a **Descrição** (texto), selecione o **Tipo** (A Pagar / A Receber), **Valor** (numérico, ex: 150,00), **Data de Vencimento** e a **Categoria**.
  - *Dar baixa:* Na lista de contas, encontre a conta desejada e clique no ícone de "Check" (confirmar). Isso converte automaticamente a conta em um Lançamento Realizado.
- **Lançamentos Realizados:** Todo o histórico de fluxo de caixa (entradas de loja, pagamentos de assinatura, gastos com equipe/marketing). Você pode adicionar um lançamento avulso clicando em **"+ Novo Lançamento"**.
- **Categorias Financeiras:** Todo lançamento precisa estar associado a uma categoria (ex: "Assinaturas", "Marketing", "Infraestrutura"). Se a categoria que você precisa ainda não existir na lista, clique em **"+ Nova Categoria"** (ao lado de "Novo Lançamento"), preencha o **Nome**, escolha o **Tipo** (Receita ou Despesa) e o **Grupo do DRE** — ela já aparece disponível no lançamento na hora.
- **DRE Gerencial:** Um relatório demonstrativo de resultados que categoriza as receitas e deduz os custos, entregando automaticamente a sua **Margem de Lucro** mensal. Basta selecionar o mês desejado no filtro do topo.

---

## 6. Mentorados (O Cliente)
Acompanhamento detalhado do desenvolvimento de quem contratou a SAW.
- **Listagem e Status:** Visão geral de todos os mentorados ativos e inativos, plano contratado e negócio. Daqui é possível criar novos acessos ou importar a carteira em lote via CSV.
- **Ficha do Mentorado:** Cada mentorado tem um perfil contendo seu crescimento percentual, bio, ferramentas concluídas e todo o histórico. Clicando em **"Editar"** na lista, você preenche ou atualiza diretamente o telefone, a bio, as áreas de interesse e a foto do mentorado — sem depender dele logar e preencher esses dados por conta própria.
- **Metas e Plano de Ação (Encaminhamentos):** O mentorado define suas próprias metas e cadastra as "tarefas de casa" (encaminhamentos) que precisa cumprir até a próxima sessão, sinalizando o peso de cada uma pra compor a barra de progresso. Nas abas **"Metas"** e **"Tarefas"**, no topo da tela de Mentorados, você acompanha tudo isso de forma consolidada — todos os mentorados numa lista só — e importa dados em lote via CSV (ver seção 11), com o resultado aparecendo imediatamente na listagem.

---

## 7. Mentorias e Inteligência Artificial
A gestão da entrega do serviço propriamente dito.
- **Agenda (Como agendar):** Na página de Mentorias, clique em **"+ Agendar Mentoria"** no canto superior direito. Escolha o **Tipo** (Individual ou Em Grupo). Preencha o Mentor responsável, a **Data e Hora**, a **Duração** e cole o **Link da Transmissão** (ex: URL do Google Meet).
- **Atas Geradas por IA:** Após o término da reunião, o status da mentoria muda. Acesse a Mentoria, clique na aba **"Ata"** e depois no botão **"Sugerir Resumo com IA"**. O sistema usará a inteligência artificial para preencher o **Resumo Automático** e a **Lista de Tarefas** sugeridas.
- **Revisão Humana:** O mentor tem a chance de revisar o texto na caixa de edição e deletar ou adicionar tarefas. Após validar tudo, clique no botão final **"Aprovar e Publicar"**. Só depois disso o Mentorado poderá ver o resumo no painel dele.

---

## 8. Biblioteca de Conteúdos e Loja (Cross-sell)
Monetização e entrega de valor assíncrono.
- **Conteúdos (Materiais):** Upload de vídeos (com rastreamento de minutos assistidos), documentos, apresentações e planilhas. É possível restringir acesso baseando-se no Plano do mentorado (Gratuito, Básico, Essencial, etc.).
- **Loja Integrada:** Venda de produtos extras (ex: Planilhas premium, Cursos avulsos, Ingressos VIP). O mentorado consegue adicionar ao carrinho e comprar diretamente por dentro da plataforma, gerando receita (e registros no módulo financeiro) automaticamente.

---

## 9. Eventos e Mural de Avisos
Comunicação ativa com toda a base de clientes.
- **Avisos:** Crie comunicados que aparecerão no topo do painel do mentorado assim que ele fizer login (útil para avisos de manutenção, novas funcionalidades, etc).
- **Eventos:** Cadastro de encontros ao vivo (online) ou presenciais (workshops). O sistema controla o limite de vagas e permite que o mentorado se inscreva com um clique.

---

## 10. Gestão de Time (Configurações)
O controle de quem pode fazer o que no sistema.
- **Colaboradores:** Adicione sua equipe.
- **Matriz de Permissões (RBAC):** Baseado na área que você atribuir a um colaborador (ex: Marketing), ele terá acesso **apenas** às funcionalidades pertinentes à sua função.
- **Desempenho:** Acompanhe quantas mentorias cada colaborador entregou no mês.

---

## 11. Importação em Massa (Migração)
Não é necessário recadastrar tudo do zero. Em **todos os módulos** (Mentorados, Produtos, Avisos, Colaboradores, etc.) existe um menu de ferramentas no topo da lista.

**Passo a passo para importação:**
1. Vá até a tela do item que deseja cadastrar (ex: Lista de Mentorados).
2. Na barra superior (ao lado da barra de busca), procure pelo botão **"Exportar CSV"**. Clique nele para baixar a planilha modelo e ver exatamente como o sistema espera os nomes das colunas.
3. Abra a planilha baixada (no Excel ou Google Sheets) e preencha suas linhas prestando atenção nos formatos: E-mails sem espaços extras, valores numéricos no padrão correto (ex: 15.5) e palavras que definem Status ou Categoria exatamente como o sistema usa. Salve o arquivo como **CSV (Separado por vírgulas)**.
4. Volte ao sistema e clique no botão **"Importar CSV"** ao lado do botão de exportar. 
5. Selecione o arquivo CSV salvo no seu computador.
6. O sistema processará tudo de uma vez ("Tudo ou Nada"): se houver algum erro de preenchimento numa linha da planilha (exemplo: e-mail inválido), a tela mostrará uma mensagem vermelha apontando a **linha exata** e o motivo do erro. Corrija na planilha e reenvie. Se estiver tudo certo, mostrará uma mensagem verde de sucesso.

---
*Fim do documento gerado para designação.*
