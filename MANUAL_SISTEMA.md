# SAW HUB — Manual do Sistema

Bem-vindo ao **SAW HUB**! Este manual apresenta uma visão geral de todas as funcionalidades disponíveis na plataforma, desenvolvida para centralizar a gestão da Escola de Restaurantes. O sistema é dividido em módulos específicos para cada área do seu negócio.

---

## 1. Acesso ao Sistema
A plataforma possui dois tipos principais de acesso:
- **Painel Administrativo:** Acessado por você (Fundador) e sua equipe (Colaboradores). O que cada pessoa da equipe enxerga depende da sua **Área** (Comercial, Marketing, Gestão de Performance). O Fundador (Admin Geral) enxerga tudo.
- **Painel do Mentorado:** Acessado pelos clientes. Eles têm uma visão simplificada e focada no desenvolvimento deles (acesso aos materiais, agenda de mentorias, plano de ação e loja).

**Dica de Apresentação:** Para acesso rápido em demonstrações, adicione `?demo=1` ao final da URL na tela de login (ex: `http://localhost:5173/login?demo=1`) para exibir botões de preenchimento automático das senhas de teste.

---

## 2. Dashboard Consolidado (Visão Geral)
Logo ao entrar com permissão total, você acessa o Dashboard Consolidado, que reúne os indicadores vitais do negócio em tempo real:
- **Saúde Financeira:** MRR (Receita Recorrente Mensal), taxa de Churn (cancelamentos), ticket médio e inadimplência.
- **Funil Comercial:** Quantidade de Leads ativos em cada etapa da negociação e taxa de conversão do mês.
- **Engajamento Operacional:** Total de mentorias realizadas no mês e nível de engajamento médio dos mentorados com a plataforma.

---

## 3. Módulo Comercial (Vendas)
O coração da sua captação de clientes.
- **Gestão de Leads:** Interface em estilo Kanban (colunas) para você arrastar e acompanhar os leads desde o primeiro contato até o fechamento ou perda da negociação.
- **Métricas Individuais:** Consultores de vendas podem ver a própria meta do mês, quantos fechamentos realizaram e a porcentagem de batimento da meta.
- **Importação de Leads:** É possível importar grandes volumes de contatos antigos através de planilhas CSV.

---

## 4. Módulo Financeiro
Controle total do caixa da empresa sem precisar de planilhas externas.
- **Contas a Pagar e Receber:** Acompanhe o que está pendente, o que está atrasado e dê baixa rapidamente.
- **Lançamentos Realizados:** Todo o histórico de fluxo de caixa (entradas de loja, pagamentos de assinatura, gastos com equipe/marketing).
- **DRE Gerencial:** Um relatório demonstrativo de resultados que categoriza as receitas e deduz os custos, entregando automaticamente a sua **Margem de Lucro** mensal.

---

## 5. Mentorados (O Cliente)
Acompanhamento detalhado do desenvolvimento de quem contratou a SAW.
- **Listagem e Status:** Visão geral de todos os mentorados ativos e inativos, plano contratado e negócio. Daqui é possível criar novos acessos ou importar a carteira em lote via CSV.
- **Ficha do Mentorado:** Cada mentorado tem um perfil contendo seu crescimento percentual, bio, ferramentas concluídas e todo o histórico.
- **Metas e Plano de Ação (Encaminhamentos):** Você pode definir metas claras e cadastrar as "tarefas de casa" (encaminhamentos) que o mentorado precisa fazer até a próxima sessão. O sistema acompanha o peso de cada tarefa para gerar a barra de progresso.

---

## 6. Mentorias e Inteligência Artificial
A gestão da entrega do serviço propriamente dito.
- **Agenda:** Criação de mentorias Individuais e em Grupo, com link de transmissão (ex: Google Meet) e data.
- **Atas Geradas por IA:** Após o término da mentoria, o sistema usa IA para ler a transcrição da reunião e gerar, em segundos, um **Resumo Automático** e **Sugestões de Tarefas** para o mentorado.
- O mentor tem a chance de **revisar e aprovar** as sugestões da IA antes de publicar a Ata oficial para o cliente ler.

---

## 7. Biblioteca de Conteúdos e Loja (Cross-sell)
Monetização e entrega de valor assíncrono.
- **Conteúdos (Materiais):** Upload de vídeos (com rastreamento de minutos assistidos), documentos, apresentações e planilhas. É possível restringir acesso baseando-se no Plano do mentorado (Gratuito, Básico, Essencial, etc.).
- **Loja Integrada:** Venda de produtos extras (ex: Planilhas premium, Cursos avulsos, Ingressos VIP). O mentorado consegue adicionar ao carrinho e comprar diretamente por dentro da plataforma, gerando receita (e registros no módulo financeiro) automaticamente.

---

## 8. Eventos e Mural de Avisos
Comunicação ativa com toda a base de clientes.
- **Avisos:** Crie comunicados que aparecerão no topo do painel do mentorado assim que ele fizer login (útil para avisos de manutenção, novas funcionalidades, etc).
- **Eventos:** Cadastro de encontros ao vivo (online) ou presenciais (workshops). O sistema controla o limite de vagas e permite que o mentorado se inscreva com um clique.

---

## 9. Gestão de Time (Configurações)
O controle de quem pode fazer o que no sistema.
- **Colaboradores:** Adicione sua equipe.
- **Matriz de Permissões (RBAC):** Baseado na área que você atribuir a um colaborador (ex: Marketing), ele terá acesso **apenas** às funcionalidades pertinentes à sua função.
- **Desempenho:** Acompanhe quantas mentorias cada colaborador entregou no mês.

---

## 10. Importação em Massa (Migração)
Não é necessário recadastrar tudo do zero. Em **todos os módulos** existe a opção "Importar CSV". Basta adequar os dados no formato aceito pelo sistema e enviar o arquivo. O sistema foi programado para ser "Tudo ou Nada": caso haja um erro em uma linha da planilha, ele aponta o erro sem poluir o banco de dados.

---
*Fim do documento gerado para designação.*
