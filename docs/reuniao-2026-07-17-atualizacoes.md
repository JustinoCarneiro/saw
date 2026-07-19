# Reunião 17/07/2026 — Atualizações e novos requisitos

> Fonte: transcrição da reunião com Victor Oliveira (Saw Escola de Restaurantes) em 17/07/2026,
> 10:02 GMT-03:00. **A transcrição colada foi cortada pelo limite de caracteres antes do fim**
> (parou no meio da pergunta sobre Planos & Ranking) — alguns itens abaixo estão marcados como
> "resposta cortada/não capturada" por causa disso, não porque o cliente não respondeu.
>
> Prazo combinado: apresentar progresso/resultado ao Victor até **quarta-feira, 22/07/2026**.
> Contexto importante: **todo o MVP já está implementado** (M04–M22 no ROADMAP.md, todos
> `✅ Concluído`). Isso muda a natureza do trabalho — a partir daqui é *change request* em cima de
> módulos em produção, não construção do zero. Antes de mexer em schema já usado, vale confirmar
> os itens marcados como "em aberto" abaixo.

---

## ✅ Resolvido: Planos vs. Tipos de contrato

**Confirmado pelo Victor:** *"não existem planos, mas sim produtos."* A hierarquia
Gratuito/Básico/Essencial/Profissional (`Plano`, usada hoje em E9/E10/E11/E15) deixa de existir
como conceito de negócio — substituir por **produto/tipo de contrato contratado**:

1. **Mentoria Contínua** — 12 meses. Reunião semanal em grupo (gravada, ata única disparada pra
   todos) + 2 encontros individuais no ano com o Mateus (diagnóstico inicial + acompanhamento),
   com um 3º de bônus opcional. Direito a 3 eventos gratuitos/ano.
2. **Mentoria Individual** — 12 meses. Mesmo escopo da Contínua, mas com reunião individual
   **mensal** em vez de só duas por ano.
3. **Consultoria** — sem acesso à mentoria contínua; reunião **semanal** individual, ata e
   encaminhamento cobrado pela Leia. **Não tem duração fixa** — confirmado como *"esporádica,
   diferente das mentorias e mentorias contínuas"* (os números de "3 meses"/"6 meses" ditos em
   momentos diferentes da reunião não se aplicam; não fixar prazo de vencimento pra esse tipo).

**Impacto técnico:** isso é uma migração de schema, não só de tela — o enum `Plano` está
amarrado a E9 (perfil/assinatura), E10 (distribuição), E11 (cadastro), E15 (gating de módulo).
Vira `TipoContrato` (ou nome equivalente) com regra de vencimento por tipo (12 meses fixos pra
Contínua/Individual, sem vencimento calculado pra Consultoria). Módulo de maior risco de
retrabalho no MVP atual — tratar como o item técnico mais delicado da leva.

---

## Por épico

### E11 · Gestão Admin (Onboarding) — M06, concluído
- **[CONFIRMADO]** Poder criar mentorado direto, sem passar por Lead, virando automaticamente
  "lead fechado". Resposta explícita do cliente: *"É IMPORTANTE PODER CRIAR DIRETAMENTE O
  MENTORADO, SENDO AUTOMÁTICAMENTE UM LEAD FECHADO."* Validar o fluxo atual (H11.1) e adicionar
  atalho "Criar mentorado" com sub-opção "puxar de um lead existente" ou "criar direto".
- **[CONFIRMADO]** Vendedor de um lead = **apenas Consultor Comercial** (não Fundador/Admin, que
  hoje entra na lista só pra não travar telas sem time comercial cadastrado — remover esse
  fallback quando o cadastro de Consultor Comercial estiver garantido).
- **[CONFIRMADO]** Senha do mentorado: trocar o fluxo manual (Admin gera e repassa por WhatsApp/
  e-mail) por **link automático de "criar sua senha" enviado por e-mail**. Vale para quando a
  área do mentorado for reaberta (hoje pausada, ver seção "Pausado nesta leva" abaixo).
- **[NOVO]** Diagnóstico inicial (feito pela Leia antes da 1ª reunião com o Mateus) como
  formulário/registro estruturado — hoje só existe numa aba do Notion.
- **[CONFIRMADO]** Mentor: acesso à plataforma é só da equipe interna SAW (Leia, Mateus, equipe).
  Porém **existem professores/mentores que não terão login na plataforma** e mesmo assim
  precisam ficar registrados no sistema (dado cadastral, sem conta de usuário) — cadastro de
  mentor não pode exigir vínculo 1:1 com um `User`/login.

### E5 · Mentorias & Atas — M06 (Admin) + M12 (mentorado), concluído
- **[MUDANÇA GRANDE]** Descontinuar a transcrição via Whisper/IA externa (custo) — usar a
  transcrição nativa do Google Meet. Isso conflita com o diferencial de MVP descrito no
  `CLAUDE.md` (upload de áudio → Whisper → IA gera rascunho de ata). **Repensar o pipeline**: em
  vez de subir áudio bruto, importar/colar a transcrição que o Meet já gera, mantendo só a etapa
  de "gerar rascunho de ata a partir de texto" via IA — o diferencial de IA muda de forma, não
  desaparece.
- **[NOVO]** Campo **"Decisões"** na ata, além de pauta e encaminhamentos (hoje a estrutura tem
  data/hora/local, participantes, pauta, encaminhamentos).
- **[NOVO]** Reorganizar a lista de Mentorias: mentorias em grupo (Contínua, semanal) numa lista
  central única; mentorias individuais/consultoria atreladas à aba do próprio mentorado, sem
  misturar na lista geral.
- **[CONFIRMADO]** Ata de mentoria em grupo é a mesma para todos os participantes (resolve
  Suposição #3 do `spec.md`).
- **[NOVO]** Controle de participação em aula semanal (nº da aula, tema, presença sim/não) —
  alimenta o painel consolidado (correlacionar crescimento x frequência).
- **[NOVO]** Controle de vagas em evento por mentorado da Contínua (direito a 3 eventos
  grátis/ano) — decrementar vaga ao inscrever/comparecer.
- **[NOVO]** Frequência/presença do mentorado precisa aparecer na visão que o time interno usa
  pra acompanhá-lo (aba/perfil do mentorado no Admin — ver rename para "Gestão de Performance"
  abaixo), não só como dado bruto.
- Zoom/outra plataforma além do Meet não foi pedido — Suposição #5 do `spec.md` segue como
  "só Google Meet", sem reconfirmação explícita nesta reunião.

### Renomear "Mentorados" → "Gestão de Performance" (Admin)
- **[NOVO]** O cliente pediu explicitamente pra trocar o nome da aba/seção "Mentorados" (hoje
  `/admin/mentorados`, módulo `MENTORADOS`) para **"Gestão de Performance"** — é o nome que a
  área já usa internamente (mesmo nome da área de RBAC do E15). Mudança de label na Sidebar/
  Shell, não muda rota nem módulo. Ainda não implementado nesta leva — só registrado aqui.

### E13 · Comercial & Vendas — M05, concluído
- **[NOVO]** Formulário único de venda que distribui o dado automaticamente: valor + forma de
  pagamento → Financeiro; dados do lead (nome/empresa/e-mail/telefone) → CRM; dados de
  credenciamento (setor, almoço) → Eventos. Hoje são 2-3 planilhas manuais separadas.
- **[NOVO]** Campos condicionais: ao selecionar produto = "Ingresso de evento", abrir campos
  extras (qual evento, categoria — Individual/Duplo/VIP, dados de credenciamento). Outros
  produtos não mostram esses campos.
- **[NOVO]** Funil de vendas cobrindo todos os produtos (mentoria, consultoria, produto digital,
  ingresso), não só mentoria.
- **[NOVO]** Campo "origem/canal da venda": direto (comercial), Hotmart, cortesia, patrocinador,
  palestrante etc.
- **[NOVO]** Parcelamento na venda (entrada + N parcelas) → gera recorrência de contas a receber
  no Financeiro automaticamente.
- **[NOVO]** Separar "valor total do contrato" de "valor pago até agora" (ex.: mentoria de R$26k
  com entrada + parcelas).
- **[MUDANÇA]** Venda de ingresso **não** conta na meta/comissão da vendedora no mês da venda —
  só conta quando o evento acontece. Dashboard precisa separar "vendido no mês" (sem ingresso) de
  "venda de ingresso" (por evento, contabilizado na ocorrência do evento).
- **[NOVO]** Dashboard comercial mais visual — venda de ingresso por evento selecionável
  (vendidos vs. total do evento, incluindo cortesias), venda "por fora" (produto + valor).
- **[NOVO]** Importação de planilhas de eventos passados pra popular histórico.
- **[BACKLOG — cliente pediu pra adiar]** Loja separada para leads (não-mentorados) que compraram
  ingresso. *"Vamos focar na gente e nos mentorados que já estão"* — não implementar agora.

### E14 · Financeiro & DRE — M04, concluído
- **[NOVO]** Filtro/aba mensal para contas a pagar/receber.
- **[NOVO]** Despesas fixas vs. variáveis, com subcategorias (eventos, pessoal, estrutura,
  operação, financeiro, jurídico) permitindo comparativo — afeta o modelo de
  `CategoriaFinanceira` do M04.
- **[NOVO]** Merge entre "lançamentos" e "contas a pagar/receber" — hoje são conceitos separados
  (`POST /lancamentos` vs `POST /contas`). Precisa desenho técnico antes de mexer em schema já em
  produção — pode ser resolvido como visão/relatório unificado em vez de merge de entidade.
- **[NOVO]** Renomear a aba "Faturamento" para "Dashboard" (mudança de tela, o endpoint
  `dashboard-faturamento` pode manter o nome).
- **[NOVO]** Dados do mentorado (forma de pagamento, mensalidade, data de início) alimentando
  contas a receber automaticamente — depende do parcelamento capturado no formulário de venda
  do E13.
- **[NOVO]** Conciliação entre valor total do contrato e valor efetivamente recebido
  (parcela a parcela) — importante pra declaração de imposto.
- **[NOVO]** Receita/despesa de evento rastreada por evento específico, independente do mês do
  gasto — conecta E13 (venda de ingresso) ao financeiro.
- **[PARCIALMENTE RESPONDIDO]** DRE precisa de "mais gráficos e detalhe que estão nas planilhas
  do financeiro" — direção confirmada, mas o detalhe exato depende da planilha real que o Victor
  vai compartilhar (ver seção "Como me passar a estrutura" no fim deste documento).
- **[RESPONDIDO]** O time **não sabe** que "Reembolsar pedido" não aciona o Mercado Pago de
  verdade — mas isso deixou de ser urgente porque a **Loja está pausada** nesta leva (ver seção
  abaixo). Retomar esse alinhamento com o time quando a Loja for reativada.

### E17 · Painel Consolidado & Ranking — M03 (implementado sem Blueprint formal)
- **[NOVO]** Gamificação anual com dois modelos de premiação: (a) por faixa de faturamento
  (R$100k a R$1M) e (b) por "liberdade"/organização — implementação de manual de processos,
  cultura, CMV e DRE estruturada.
- **[NOVO]** Lista de "ferramentas obrigatórias" com nomes confirmados: **DRE, manual de cultura,
  ficha técnica, manual de processos** — resolve a Suposição #8 do `spec.md` (antes só 3 itens
  citados, agora são 4).
- **[NOVO]** Dashboard consolidado com aba por mentorado + visão global (pro Mateus), com filtro
  por mentorado e por área (Gestão de Performance) — validar se o M03 atual já tem esse nível de
  filtro/navegação.
- **[CONFIRMADO indireto]** Indicadores mostrados no modelo Canva usado hoje (caso "J Crocs"):
  faturamento, número de pedidos, % de execução de tarefas/encaminhamentos.

### E9 · Perfil & Gamificação — M15, concluído
- Diretamente afetado pela decisão crítica de Planos vs. Tipos de contrato (H9.3 hoje mostra
  "plano atual" com upgrade/downgrade — não faz sentido pros tipos de contrato de duração fixa
  descritos na reunião).

### E15 · Gestão de Time — M02/M19/M20, concluído
- **[REFORÇADO, sem mudança de código]** Acesso ao sistema de mentoria restrito à equipe interna
  (hoje: Leia, Mateus, equipe) — já é o modelo de RBAC do E15; a reunião confirma que o desenho
  atual está correto, não pede mudança.
- Ver item "quem pode ser mentor" em E11 acima (respondido: só equipe interna loga; mentores/
  professores sem login também precisam existir como cadastro).

---

## Notion → SAW HUB (migração de dados)
- Cliente vai listar e enviar todos os recursos oferecidos aos mentorados (DRE, manual de
  cultura, ficha técnica, manual de processos) — usar pra popular Materiais (E6) e a lista de
  "ferramentas obrigatórias" do E17.
- Victor comprometeu-se a compartilhar as planilhas do comercial e o Notion dos mentorados —
  usar como fonte de import. E13 já tem import CSV (M22); avaliar se o formato serve ou se
  precisa adaptar ao layout real das planilhas mostradas na reunião.
- Hoje atas/atividades são centralizadas no Notion — o objetivo declarado da reunião é esse
  controle migrar pro SAW HUB ("sistema nervoso das operações").

### Estrutura real encontrada no Notion (17/07/2026) — database "CRM Saw"

Explorado dentro de "Gestão de Performance" → "CRM Saw" (não estava visível de cara; é a base
real por trás da "aba do mentorado" que a Leia demonstrou na reunião). Views: **Visão CS**,
**Mentoria**, **Mentoria Contínua**, **Fórmula SAW**, **Visão Comercial** — cada view parece
filtrada por `Produto/serviço`, o que sugere um **4º produto real, "Fórmula SAW"**, não coberto
pelos 3 tipos de contrato já confirmados (Mentoria Contínua/Individual/Consultoria) — **precisa
confirmar com o Victor** se é um produto à parte ou outro nome pra um dos três já mapeados.

Campos de topo (linha da tabela / topo do card), por mentorado:
- `Nome`, `Produto/serviço` (Mentoria Contínua etc.), `Tipo` (Cliente ativo / Pausado),
  `Vencimento` (número — provável dia de cobrança; um registro tinha "Pago Total" no lugar de
  número, confirmar a regra), `Semáforo de acompanhamento`, `Próxima ação` (texto livre),
  `Frequência` (tag "FALTOU" — controle de presença), `Status diagnóstico` (ex.: "Primeiro
  diagnóstico" — sugere um fluxo de estágios, não é booleano) — e mais **16 propriedades ainda
  não vistas** (link "16 more properties" dentro do card, a expandir).

Dentro do card de cada mentorado, dois blocos de texto estruturado:
- **"Dados do contrato"**: sócios (**mais de um por empresa**, ex.: "Girlandia Aragão de Sousa"
  + "Jaene Oliveira de Araujo" no caso visto), CNPJ, valor do contrato (ex.: R$ 26.000 — bate
  com o "R$26 mil" citado na reunião pra Mentoria Contínua), data de fechamento, telefone,
  e-mail — mais um embed de PDF do contrato assinado.
- **"Diagnóstico Inicial"**: faturamento anual, nº de colaboradores, empresa regularizada
  (sim/não), nº de lojas, CMV atual (sim/não + qual), tempo médio de atendimento, cultura
  construída (sim/não/**em construção**), processos desenhados (sim/não/**em construção**) — ou
  seja, pelo menos 2 desses campos não são booleanos simples, têm um 3º estado intermediário.

**Ata real conferida** (PDF "ATA DE REUNIÃO 07/06/2026"): estrutura é **Participantes** (lista),
**Pauta** (lista), **Encaminhamentos** (lista) — confirma que falta mesmo a seção "Decisões" que
já tínhamos identificado como novo requisito.

**As 16 propriedades restantes do card** (expandidas em "Menu Caseirinho novo", quase todas
`Empty` neste registro): `Dono e sócio`, `Fechamento`, `Valor contrato`, `Origem de contato`,
`Status` (tag "Entrar em contato" — parece resíduo de um funil tipo lead, inconsistente com
`Tipo`=Cliente ativo do mesmo registro), `Valor final`, `Telefone`, `Email`, `Nome Fantasia`,
`Data`, `Observações`, `Caixa de seleção` (checkbox), `Selecionar`, `Origem`, `Tamanho`.

**⚠️ Achado crítico sobre a qualidade do dado**: os campos estruturados acima (`Dono e sócio`,
`Fechamento`, `Valor contrato`, `Telefone`, `Email`) estão **vazios** neste registro — mas o
mesmo card mostra "Dados do contrato" com sócios, CNPJ, valor (R$ 26.000), data de fechamento,
telefone e e-mail preenchidos **como texto livre dentro de um callout**, não nas propriedades da
database. Ou seja: a estrutura de campos existe e está bem pensada, mas **a equipe preenche o
dado real como anotação de texto corrido**, não nos campos — um export/CSV das propriedades da
database viria com essas colunas quase todas em branco. O dado de verdade está espalhado em
texto livre por página, com formatação que pode variar registro a registro.

**Ata é arquivo solto por página, não database central**: confirmado — a ata
(`ATA_DE_REUNIO_07062026.pdf`) está anexada direto na página do mentorado "Menu Caseirinho", não
como relation pra uma database de atas. Cada mentorado tem seus próprios PDFs soltos na própria
página — não tem como "puxar todas as atas de uma vez" de um lugar central, view por view. A
mesma página também tem um bloco de **"Central de Tarefas" colado (não filtrado)** — mostra
tarefas de outros clientes (Della Pizza, Point Lanches 085, Jay Croc) misturadas, então não é um
link genuíno por mentorado, é a base inteira copiada ali — não confiar nesse bloco como fonte de
encaminhamentos daquele mentorado específico.

**Conclusão da investigação**: a "CRM Saw" tem o desenho de campo certo (dá pra usar como
referência de schema pro cadastro do mentorado no E11 — ver lista de campos acima), mas **não é
uma fonte confiável de import automático** — o dado de verdade está em texto livre e arquivos
soltos, não em propriedades estruturadas, e varia de formatação por registro. Decisão de como
prosseguir (reentrada manual guiada por planilha-modelo vs. tentar parsear o texto livre) ainda
em aberto — só registrando o levantamento por enquanto.

### Documentação de processo encontrada (página "PADRONIZAÇÕES" + PDFs anexos)

Diferente do "CRM Saw" (dado por mentorado), isso é **metodologia/processo** — não migra como
dado, mas refina os requisitos já levantados. Fontes: página "PADRONIZAÇÕES" dentro de "Gestão
de Performance", + `Fluxograma.pdf` e `fluxograma_aline_comercial.pdf` anexados nela.

- **Nome confirmado pelo Marcos**: quem faz o acompanhamento do mentorado (papel "Sucesso do
  Gestor") é a **Leia** — a grafia certa (a transcrição da reunião e o PDF `Fluxograma.pdf`
  grafaram diferente em pontos distintos; mantendo "Leia" como padrão neste documento a partir
  daqui).
- **Modelo de engajamento/risco que preenche o "Semáforo de acompanhamento" vazio no CRM Saw**:
  o processo de "Check-in Individual" (usado quando um mentorado contínuo é identificado como
  "desengajado") termina em uma análise pós-check-in com dois eixos formais: **Nível de
  engajamento** (Alto/Médio/Baixo) e **Risco de churn** (Não/Atenção/Alto). Isso é mais rico que
  o "Em dia/Atenção/Atrasado" que o `spec.md` previa pro E17 (H17.1) — **propor incorporar os
  dois eixos** (engajamento + risco de churn) em vez de um status único.
- **Funil comercial real tem uma etapa a mais que o `spec.md`**: `fluxograma_aline_comercial.pdf`
  mostra Prospecção ativa → Contato → Lead respondeu? → **Diagnóstico** (agendado, depois
  realizado — Aline faz 5 perguntas: tempo de negócio, tem sócio, tamanho da equipe, faturamento
  médio, principal dor dentre "6 pilares") → Definição do produto → Proposta → Fechou?/Follow
  up. O `spec.md` (H13.2) hoje só tem `Em contato → Proposta → Fechado/Perdido` — falta a etapa
  de **Diagnóstico** entre contato e proposta.
- **"Fórmula de Sucesso" provavelmente é o kit de ferramentas do onboarding, não um 4º produto**:
  o `Fluxograma.pdf` usa a frase "apresenta as ferramentas da Fórmula de Sucesso" como parte da
  etapa de onboarding (feita pela Leia), não como um produto vendido à parte. Isso enfraquece a
  hipótese de "Fórmula SAW" ser um 4º tipo de contrato (ver seção CRM Saw acima) — mais provável
  que a aba "Fórmula SAW" do CRM Saw seja sobre esse kit/metodologia, não sobre um contrato
  distinto. Ainda assim, **confirmar com o Victor** antes de descartar de vez.
- **Fluxo completo de onboarding, com responsável por etapa** (útil pro desenho de tela do E11):
  1. Fechamento da venda — **Aline** (Comercial).
  2. Áudio de boas-vindas — **Matheus** (Fundador/mentor).
  3. Onboarding direcionado + agendamento do 1º encontro + apresentação das ferramentas —
     **Leia**.
  4. Levantamento de necessidades — **Leia** monta um PDF com cenário atual, respostas às
     perguntas do Victor, prints (Google reviews — 3 piores avaliações, perfil iFood, cardápio
     iFood/site, Instagram), faturamento e nº de colaboradores.
  5. Diagnóstico + plano de desenvolvimento anual — **Matheus**, a partir do material da Leia.
  6. Acompanhamento semanal (follow-up) — **Equipe de Sucesso do Gestor**.
- **Ata é mesmo distribuída via WhatsApp, não centralizada**: a própria página confirma o
  processo — Victor estrutura a ata em PDF após a reunião e envia no grupo do WhatsApp do
  mentorado, seguida de uma mensagem separada com os encaminhamentos (marcados com ✅ conforme
  concluídos). Bate com o padrão "PDF solto por página" que vimos no CRM Saw — não existe (nem é
  esperado que exista) uma database central de atas hoje.
- **Baixa relevância pra esse levantamento**: a "Ata reunião de alimento SAW" (09/03/2026) é ata
  *interna* do time (escala de reunião, viagem, dia de marketing), não de mentorado — e
  "Solicitar sempre aos mentorados"/"Demandas solicitadas pela Verica" são notas soltas de baixa
  prioridade (parceiros de saúde mental/imagem pessoal).

## Planilhas reais do Comercial/Financeiro (Google Sheets, 17/07/2026)

Diferente do Notion, essas planilhas estão **estruturadas de verdade** (colunas fixas, dropdowns,
fórmulas somando totais) — dá pra usar como base direta de schema/import pro E13 e E14. Marcos
confirmou: **são conectadas entre si** — a "DRE Financeira Saw" puxa totais que vêm das
planilhas de evento/vendas, não são fontes independentes.

### 1. "DRE Financeira Saw" (E14 — Financeiro)
Abas mensais (MAR 26, ABR 26, MAI 26, JUN 26, JUL 26 — confirma o pedido de "aba mensal").
- **Caixa do mês**: Inicial, saldo por banco (Itaú, Infinity Pay), Final.
- **Receita** (resumo): Total, Fixas, Variáveis, Eventos, Mentoria Contínua, Mentoria Individual,
  Patrocínio, Produtos Digitais.
- **Despesa** (resumo): Total, Fixas, Variáveis, Eventos, Pessoas, Estrutura.
- **Tabela "Despesas"** (lançamento a lançamento): `Data`, `Tipo` (Despesas), `Categoria`
  (Estrutura/Pessoas/Operação/Financeiro — **bate com o que o Victor pediu na reunião**),
  `Subcategoria` (ex.: Água Mineral, Almoço Administrativo, Mobiliário, Sistemas, Aluguel,
  Brindes Evento, Alimentação e Transporte, Cartão de Crédito, Limpeza, Design, Equipamentos,
  Energia, Internet — **esse é o plano de contas real que faltava**, resolve a pergunta em
  aberto sobre categoria financeira pré-cadastrada), `Tipos de Despesa` (Fixa/Variável),
  `Detalhamento` (texto livre), `Valor`, `Forma de Pagamento` (Pix/Cartão/Boleto), `Banco`.
- **Tabela "Receitas"**: `Data`, `Tipo` (Receita), `Categoria` (Vendas/Eventos/Mentoria/
  Patrocínio), `Subcategoria`, `Natureza` (Fixa/Variável), `Detalhamento` (nome do cliente),
  `Valor`, `Forma de Pagamento`.
- **Achado novo nos prints (18/07/2026)**: a aba `JUL 26` tem uma tabela de receita renomeada
  pra `Tabela_1` (era `Receitas_5`) com uma coluna `Parcela` nova (valores tipo `1/10`, `4/11`,
  `2/6`) — parcelamento estruturado de verdade, diferente do texto livre em "Observações" visto
  em "Vendas Aline Melo". Ainda não confirmado se é um padrão novo (só a partir de julho) ou só
  não reparei nas abas anteriores.
- **"Transferências Entre Contas"/"Transferências Extraordinárias"**: movimentação de dinheiro
  entre bancos (Itaú ↔ Infinity Pay) — empréstimo, transferência interna. **Não é receita nem
  despesa**, conceito que `ContaPagarReceber`/`LancamentoFinanceiro` não modelam hoje.
- Essa planilha está marcada **"Somente ver"** pro Marcos, mas **o raio-x via Apps Script rodou
  normalmente mesmo assim** — a restrição de "Somente ver" bloqueia `Arquivo → Fazer download`
  na UI, mas não bloqueia leitura via `SpreadsheetApp.openById()` com a conta que tem acesso de
  visualização (achado ao vivo, 18/07/2026: chegamos a suspeitar que bloquearia, testamos, não
  bloqueou). Antes de rodar o script, pedimos pro Gemini integrado ao Sheets uma leitura
  preliminar — ele errou ao dizer "não é possível ver fórmula" (na real, é uma limitação de como
  o Gemini processa a planilha, não do acesso em si — a barra de fórmulas do Sheets mostra
  fórmula normalmente pra quem só visualiza). Os números abaixo são os confirmados pelo raio-x
  de verdade (substituem a estimativa do Gemini, que tinha pequenas imprecisões):
  - **Zero células com erro** (`#REF!`/`#N/A`/`#DIV/0!`) em nenhuma tabela, todas as 5 abas —
    confirmado via varredura completa, não amostra.
  - **Fórmulas reais confirmadas** (ex.: `=SUMIFS(G:G;B:B;"Despesas";E:E;"Fixa")`,
    `=SUMIFS(Q:Q;L:L;"Receita";O:O;"Eventos")`) — os totais de Receita/Despesa por categoria são
    sempre `SUMIFS` sobre a tabela de lançamentos da própria aba, nunca `IMPORTRANGE` (essa
    planilha não puxa de nenhuma outra, diferente de "Eventos - Despesas e Receitas").
  - **`Status` (despesas), coluna J, só 2 valores confirmados**: `Pago`, `Falta Pagar` — sem
    "Parcial". **Diferente** das outras 2 planilhas (`FALTA PAGAR`/`PAGO PARCIAL`/`PAGO` em
    "Eventos"; `Pago`/`Parcial`/`Pendente` em "Vendas Aline Melo") — confirma que o padrão de "3
    estados" não é universal entre as planilhas do cliente, cada uma usa granularidade diferente
    pro mesmo conceito.
  - **`Categoria` (despesas), coluna C, 7 valores confirmados** (filtrado só em linhas
    `Tipo = Despesas`, sem ruído de outras tabelas da aba): `Estrutura`, `Eventos`,
    `Financeiro/Jurídico`, `Marketing`, `Operação`, `Outros`, `Pessoas`.
  - **`Forma de Pagamento` (despesas), coluna H**: `Pix`, `Boleto`, `Cartão` — sem Hotmart aqui
    (gasto interno, não venda).
  - **`Subcategoria` (despesas), coluna D — 48 valores distintos confirmados** (não 44; a conta
    do Gemini tinha uma pequena imprecisão). **Plano de contas completo, pronto pra virar seed
    do E14**:
    Alimentação Administrativa, Alimentação Evento, Almoço Administrativo, Almoço/Jantar de
    Negócios, Aluguel, Apresentador Evento, Brindes Evento, Cartão de Crédito, Combustível,
    Comercial, Condomínio, Contabilidade, Coordenador de Projetos, Custos Viagem Evento, Design,
    Diretor, Doação, Endomarketing, Energia, Equipamentos, Estacionamento, Estadia, Estorno,
    Estrutura Evento, Financeiro (Mentor), Hotel, Impostos, Internet, Jurídico, Limpeza,
    Materiais Evento, Mentoria, Midia Evento, Mobiliário, Músico Evento, Outros, Palestras
    Evento, Passagens, RH (Mentor), Sistemas, Social Media, Sucesso do Gestor, Tráfego Pago,
    Uber, V.A e V.T, Visita Pré-evento, Visitas mentorados, Água Mineral.
  - **"Transferências Entre Contas"/"Transferências Extraordinárias" tem vocabulário de status
    próprio** (`✅ Correspondente`) — é um conceito diferente (conciliação bancária, não status
    de pagamento), confirma que não é um 3º estado do mesmo enum, é campo de outra tabela.

### 2. "Vendas Eventos" (E13 — venda de ingresso)
Uma aba por evento (ex.: "A Receita do Sucesso 4 - Teresina", "A Receita do Sucesso 5 - Maceió",
"A Receita do Sucesso 6 - Natal/Salvador", "Evento Recife", "SAW Líderes"...). Colunas: `Nome do
Aluno`, `Quantidade de ingressos`, `Valor Líquido do Ingresso (R$)`, `Tipo de ingresso` —
dropdown com valores **Cortesia / Essencial / Vip / Especial** (diferente do "Individual/Duplo/
VIP" que a reunião tinha citado de memória — **usar esses 4 valores reais**), `Origem da Venda`
— dropdown com vendedor nomeado (ex.: "Aline Melo Comercial", "Matheus") + `CORTESIA` +
`HOTMART`, `Nome da Empresa`, `Telefone`, `Email`.

#### Achado grande (18/07/2026): esta é a planilha-fonte do `IMPORTRANGE`
ID confirmado: `1h8BmrO41_pWW7IIfjJ72OxnWGtRasQfEooJt_BV_jsQ` — **é exatamente o mesmo ID** que
`ARRAYFORMULA(IMPORTRANGE(...))` importava em todas as abas de "Eventos - Despesas e Receitas"
(achado anterior, ver seção 5). Mistério resolvido: "Vendas Eventos" é a fonte primária real de
venda de ingresso; "Eventos - Despesas e Receitas" só consolida um subconjunto dela pro P&L.
Tem mais abas/eventos do que a consolidada tinha exposto (Recife, Salvador, Portugal, Fortaleza).

#### Confirmado via raio-x (18/07/2026) — 10 abas, zero fórmula, zero erro
Diferente da DRE/Eventos, esta planilha é 100% dado bruto (nenhuma célula com fórmula em
nenhuma aba) — é mesmo a "folha de origem" que outras planilhas processam/importam.

- **Zero células com erro** em todas as 10 abas.
- **`Tipo de ingresso` real, 4 valores confirmados (com volume)**: `Essencial` (165 linhas),
  `Vip` (60), `Especial` (22), `Black` (3 — raro, mas real, visto só em "A Receita do Sucesso 8
  - Fortaleza", pode ser categoria de campanha específica, não confirmado se é permanente).
- **Achado que corrige código já implementado**: `Cortesia` **não é** um `Tipo de ingresso` —
  toda ocorrência de "CORTESIA" na planilha está na coluna `Origem da Venda`, nunca em `Tipo de
  ingresso`. O `CategoriaIngresso` do backend (`CORTESIA`/`ESSENCIAL`/`VIP`/`ESPECIAL`,
  implementado no M25) tem um valor no eixo errado — o real deveria ser
  `ESSENCIAL`/`VIP`/`ESPECIAL`/`BLACK`, com "cortesia" sendo uma *origem* (por que o ingresso foi
  emitido de graça), não uma *categoria* de ingresso. Um ingresso de cortesia ainda tem um tipo
  (Essencial/Vip/Especial/Black), só que com `valorLiquido = 0` e `origemVenda = CORTESIA`.
  **Pendência de correção, ainda não aplicada no código** — decisão registrada na lista
  consolidada de gaps.
- **`Origem da Venda` real, 5 valores confirmados (com volume)**: `Aline Melo Comercial` (103),
  `CORTESIA` (88), `HOTMART` (70), `Matheus Brayan` (10), `PARCEIRO` (4). Confirma que a coluna
  mistura nome de vendedora/fundador (`Aline Melo Comercial`, `Matheus Brayan`) com canal/motivo
  (`CORTESIA`, `HOTMART`, `PARCEIRO`) — mesmo gap de modelagem já registrado (`OrigemVenda` do
  M25 não tem onde encaixar nome de pessoa).

### 3. "Vendas [Nome da Vendedora]" — ex. "Vendas Aline Melo" (E13 — venda por fora)
**Uma planilha por vendedora**, abas mensais (Fevereiro, Março, Abril, Maio, Junho...). Colunas:
`Data da venda`, `Nome do cliente`, `Telefone`, `E-mail`, `Produto` — dropdown com produtos reais
vistos: **Formação Profissional, Mentoria Contínua, Ficha técnica Lucrativa** (`Formação
Profissional` é um produto que não estava em nenhum levantamento anterior — **novo item a
confirmar com o Victor**, pode ser sinônimo de Consultoria ou produto à parte), `Forma de
pagamento` (Hotmart, Pix Recorrente, Pix), `Valor total da venda (R$)`, `Valor pago no ato da
compra (R$)`, `Valor restante a pagar (R$)`, `Status do pagamento` (Pago/Parcial), `Observações`
(texto livre — ex.: "entrada de 6k + 10 parcelas de 2k", onde o parcelamento vive hoje, sem
campo estruturado).

#### Confirmado via raio-x ("Vendas Aline Melo", 18/07/2026) — 23 linhas de venda reais, 5 abas mensais
- **Zero fórmulas em toda a planilha** (não só zero erro — zero fórmula, ponto) — diferente de
  "Eventos - Despesas e Receitas" (que tinha `SUM`/`SUMIF` reais), aqui todo campo é digitado à
  mão, inclusive "Valor restante a pagar". Isso quer dizer que a consistência aritmética
  (Total − Pago = Restante) **não é garantida pela planilha**, precisa ser validada/recalculada
  no import, não só copiada.
- **`Produto` real, 3 valores confirmados nas linhas preenchidas**: `Mentoria Contínua`,
  `Formação Profissional`, **`Ficha técnica Lucrativa`** — esse terceiro não está em nenhum
  levantamento anterior nem no `ProdutoVenda` do M25 (que já tem MENTORIA_CONTINUA/
  MENTORIA_INDIVIDUAL/CONSULTORIA/FORMULA_SAW/FORMACAO_PROFISSIONAL/INGRESSO_EVENTO/
  PRODUTO_DIGITAL) — precisa de decisão: novo valor de enum, ou cai no catch-all
  `PRODUTO_DIGITAL`?
- **`Forma de pagamento` real, 3 valores**: `Pix`, `Pix Recorrente`, `Hotmart` — `FormaPagamento`
  do SAW HUB hoje só tem `PIX`/`CARTAO`/`BOLETO`/`HOTMART`, sem distinguir Pix avulso de Pix
  recorrente (assinatura). Pode ser relevante pro MRR (H14.3), já que "recorrente" é
  informação de negócio, não só forma de pagamento.
- **`Status do pagamento` real, 3 valores**: `Pago`, `Parcial`, `Pendente` — mesmo gap de 3
  estados já achado em "Eventos - Despesas e Receitas" (lá os literais eram
  `PAGO`/`PAGO PARCIAL`/`FALTA PAGAR`) — **confirma que é um padrão real de negócio, não
  peculiaridade de uma planilha**, mas os literais usados não são nem consistentes entre
  planilhas diferentes (⚠️ um motivo a mais pro formulário único do M25 já ter resolvido isso
  com enum próprio, em vez de replicar texto livre de planilha).
- **Achado de negócio, não só de schema — vendas via Hotmart têm "Valor pago no ato" menor que
  "Valor total", com "Valor restante a pagar" = 0 mesmo assim** (ex.: total R$597, pago
  R$540,35, restante R$0 — 11 das 23 linhas têm essa mesma assinatura, ~87-91% do total). A
  leitura mais provável: "Valor pago no ato" pro Hotmart é o **valor líquido que a SAW recebe
  depois da taxa da plataforma**, não o valor bruto pago pelo cliente — e "Restante = 0" está
  certo porque o cliente já pagou tudo (a diferença é taxa de plataforma, não dívida do
  cliente). **Se confirmado**, o `FecharVendaRequest`/`Lead.fecharVenda()` do M25 (que hoje
  valida só `valorPagoNoAto ≤ valorTotalVenda`, sem separar taxa de plataforma) precisa de um
  terceiro conceito — taxa/comissão retida —, senão toda venda Hotmart vai parecer "faltando
  pagar" quando na verdade está 100% quitada pelo cliente.

### 4. "CREDENCIAMENTO [Evento]" — ex. "CREDENCIAMENTO A RECEITA 7 - Salvador" (E13/E7 — evento)
Uma planilha por evento. Colunas: `Nome`, `Telefone`, `Email`, `Nome do Negócio`, `Categoria`
(Comum/Vip), `Almoço` (Sim/vazio), `Check-in`, `Compra` (Hotmart/Venda Direta Comercial).

#### Confirmado via raio-x (18/07/2026) — 1 aba ("NOMES"), 39 credenciados, zero fórmula, zero erro
- **`Categoria` real: só 2 valores** — `Comum` (33) e `Vip` (6, com espaço sobrando no fim do
  literal — `"Vip "`, mesma classe de sujeira de dado das outras planilhas). **Diferente** de
  "Vendas Eventos" (`Essencial`/`Vip`/`Especial`/`Black`, 4 valores) — hipótese: pra fins de
  credenciamento físico (fila/checagem no dia), a equipe só precisa saber "é VIP ou não", não a
  granularidade comercial completa. Se confirmado, `Comum` provavelmente é um agregado de
  Essencial+Especial (não-VIP), não um 5º tipo de ingresso novo — precisa perguntar ao cliente
  antes de mapear.
- **`Check-in` não tem nenhum valor preenchido** (0 de 39 linhas) — a coluna existe no schema
  mas não está sendo usada de fato neste evento. `VendaIngresso.checkIn` (booleano, já
  implementado) mapeia direto pra essa coluna, mas o dado real mostra que raramente é marcado.
- **`Compra` real: 2 valores** — `HOTMART` (21), `VENDA DIRETA COMERCIAL` (18) — mais genérico
  que "Origem da Venda" de "Vendas Eventos" (que nomeia a vendedora específica). Mesma pergunta
  do item acima: é uma simplificação só pra esta tela, ou uma origem de dado genuinamente
  diferente?

### 5. "Eventos - Despesas e Receitas" (E13+E14 — P&L por evento)
Confirma o pedido de "separar despesas e receitas por evento, independente do mês" — uma aba por
evento, com botão "Criar Novo Evento" (processo é literalmente duplicar uma aba-modelo). Por
aba:
- Resumo: Total Despesas (valor total/pago/falta pagar), Total Receitas (idem + falta receber),
  Total Ingressos (quantidade vendida, Vips, Essenciais, valor).
- **Despesas**: Nome da Despesa, Valor Total, `Parcela 1/2/3 (R$)`, Falta Pagar, Status do
  Pagamento.
- **Receitas**: Nome da Receita, `Valor Acordado`, `Valor Recebido 1/2/3`, Falta Receber, Status
  do Pagamento — e aqui aparece algo importante: **vendas de Mentoria Contínua feitas durante o
  evento são lançadas nessa mesma tabela** (ex.: "Continua Marcus Antonio Irmão Lanches" com
  Valor Acordado R$26.000, parcelas recebidas, Falta Receber) — confirma o que a reunião descreveu
  ("no evento é vendida a mentoria") e mostra que a conciliação valor-acordado × valor-recebido
  (pedida pro financeiro) já é praticada manualmente aqui, por parcela nomeada (não um contador
  genérico de parcelas).
- **Ingressos Vendidos**: mesma estrutura da planilha "Vendas Eventos" (item 2) — Nome do Aluno,
  Quantidade, Valor Líquido, Tipo de ingresso, Origem da Venda, Nome da Empresa, Telefone, Email.

#### Confirmado via raio-x (`docs/raio-x-planilhas.gs`, 18/07/2026) — valor + fórmula + dropdown reais, não só print
Rodado contra a planilha real (ID em `dados-cliente-notion/`, 6 abas — 5 eventos + 1 template).
Achados que substituem/reforçam a descrição acima com confirmação de fórmula, não só leitura
visual:
- **Zero células com erro de fórmula** (`#REF!`/`#N/A`/etc.) em nenhuma das 6 abas — apesar de
  "todas terem fórmula" (preocupação do Marcos), o dado está limpo.
- **Aba "Base Modelo Evendo"** é o template real (confirma "duplicar aba-modelo" acima) — schema
  limpo, sem dado de evento misturado, boa referência de shape pro import.
- **Totais são fórmula de verdade sobre tabelas nomeadas** (recurso nativo "Tabelas" do Sheets,
  não Named Range clássico — `getNamedRanges()` não enxerga, mas a fórmula mostra o nome): ex.
  `=SUM(Despesas_4[Falta Pagar (R$)])`, `=SUM(Receitas_4[[Valor Recebido 1]:[Valor Recebido 3]])`,
  `=SUMIF(D40:D141;"Vip";B40:B141)` pra somar ingresso por categoria. Confirma que "Falta Pagar"/
  "Falta Receber" são sempre calculados (Total − Parcelas recebidas), nunca digitados à mão.
  Uma linha por evento pesado (ex. Maceió) tem só ~64 células com fórmula de um total de ~1200
  células ocupadas — a maior parte do volume é dado de linha (nome/telefone/valor), não fórmula.
- **"Status do Pagamento" tem 3 estados reais** (dropdown confirmado via `getDataValidations`):
  `FALTA PAGAR`, `PAGO PARCIAL`, `PAGO` — **diferente do `StatusConta` do SAW HUB hoje**
  (`PENDENTE`/`PAGO`/`RECEBIDO`/`VENCIDO`, sem um estado "parcial" explícito). Gap real de
  modelagem a resolver antes do E14 importar esse dado.
- **"Origem da Venda" é nome de vendedora, não canal** (confirmado nos valores reais de célula:
  `"Aline Melo Comercial"`, `"HOTMART"`) — o enum `OrigemVenda` do M25
  (`DIRETA`/`HOTMART`/`CORTESIA`/`PATROCINIO`/`PALESTRANTE`) não tem onde encaixar um nome de
  pessoa. Precisa decisão: vira um catálogo de vendedoras (like `Colaborador`), ou o enum ganha
  granularidade nova.
- **`VendaIngresso` (já implementada no M25) está incompleta**: a planilha real guarda
  **Nome da Empresa, Telefone e E-mail** do comprador — a entidade atual só tem
  nomeCredenciado/setor/almoco/categoriaIngresso/checkIn. Falta estender.
- **Achado novo, muda o entendimento da fonte de dado**: todas as 6 abas têm uma fórmula
  `=ARRAYFORMULA(IMPORTRANGE("...spreadsheets/d/1h8BmrO41_pWW7IIfjJ72OxnWGtRasQfEooJt_BV_jsQ/edit"; "NomeDaAba!A2:H"))`
  — ou seja, **"Eventos - Despesas e Receitas" não é a planilha onde o dado nasce**, é um
  consolidado/dashboard que importa de outra planilha (ID acima) com abas de mesmo nome. Ainda
  não confirmado o que aquela planilha-fonte contém de diferente/adicional — próximo raio-x.

### Relacionamento real entre as 5 planilhas (confirmado 19/07/2026, cruzando os 5 raio-x)
Depois de ter os 5 arquivos `.json` do raio-x, cruzei nome/valor entre eles pra testar de
verdade se as planilhas "se falam" ou só parecem relacionadas pela descrição. Resultado: **só
uma relação é automática** (fórmula), **todo o resto é a mesma venda/pessoa redigitada à mão em
arquivos diferentes**:

- **Automática (fórmula)**: `Eventos - Despesas e Receitas` ← `IMPORTRANGE` ←
  `Vendas Eventos` — já documentado acima, confirmado por ID de planilha idêntico na fórmula.
- **Mesmo evento, digitado 2x, sem fórmula nenhuma ligando**: "Vendas Eventos" (aba Salvador,
  26 compradores) × "CREDENCIAMENTO A RECEITA 7 - Salvador" (39 credenciados) — **18 nomes batem
  exatamente** entre as duas listas (a diferença de 39 vs 26 é provavelmente
  cortesia/acompanhante, que só entra no credenciamento). Duas planilhas, dois preenchimentos
  manuais independentes, mesmo evento real.
- **Mesma venda, digitada 2x, sem fórmula nenhuma ligando**: a venda de "Luan - O casarão"
  (Formação Profissional, R$1.000, Pix) em "Vendas Aline Melo" (aba Maio) **reaparece na
  "DRE Financeira Saw"** (aba ABR 26) como duas linhas — `"1ª Parte Fórmula Luan Lucas"` e
  `"2ª Parte Fórmula Luan Lucas"`. `DRE Financeira Saw` não tem nenhuma fórmula de
  `IMPORTRANGE`/referência cruzada em nenhuma das 5 abas (confirmado via raio-x) — é 100%
  preenchida manualmente, mesmo quando o conteúdo repete outra planilha.

**Por que isso importa pro M25**: não é só a descrição verbal da reunião ("a vendedora preenche
2-3 planilhas por venda") — agora tem prova de dado real, com nome e valor batendo, de que a
mesma venda/pessoa é retrabalho manual em pelo menos 2 sistemas diferentes hoje. Reforça (com
evidência, não só relato) por que o "formulário único de venda" era o pedido nº 1 do Comercial.

### Implicações pro desenho (E13/E14)
- O **plano de contas já existe** (Estrutura/Pessoas/Operação/Financeiro + subcategorias) — não
  precisa inventar, só replicar essa lista como seed do E14.
- `Tipo de ingresso` real é **Cortesia/Essencial/Vip/Especial**, não o que a reunião sugeriu de
  memória — corrigir o requisito do E13.
- Parcelamento hoje vive só como texto livre em "Observações" (venda por fora) ou como colunas
  fixas `Parcela 1/2/3`/`Valor Recebido 1/2/3` nomeadas (evento) — **nenhuma das duas é uma
  estrutura de N parcelas dinâmica**; o SAW HUB pode (e deve) fazer melhor que isso com uma
  tabela de parcelas de verdade, em vez de replicar a limitação da planilha.
- **Novo produto a confirmar**: "Formação Profissional" apareceu como opção real de produto,
  fora dos 4 já mapeados (Mentoria Contínua/Individual/Consultoria + a dúvida da Fórmula SAW).
- Essas 5 planilhas + o "CRM Saw" do Notion cobrem, juntas, os 3 fluxos que a reunião descreveu
  (venda de ingresso, venda por fora, credenciamento) — mas vivem em arquivos e ferramentas
  diferentes hoje (Sheets vs. Notion), reforçando por que o formulário único é o pedido nº 1 do
  Comercial.
- **Gaps achados via raio-x (18/07/2026) — status em 19/07/2026: 6 de 8 implementados** (1, 2, 3,
  5, 6, 8 — todos confirmados pelo Marcos direto, sem precisar do Victor, sob a filosofia "vamos
  sempre trabalhar com gerais e se depois o cliente quiser tirar, tiramos"). Gap 4 já estava
  resolvido (mapeamento do IMPORTRANGE). Gap 7 segue **deliberadamente fora desta leva** — é uma
  pergunta sobre o que um número significa (não uma categoria a incluir), continua precisando de
  confirmação real do cliente antes de qualquer mudança de código.
  1. ✅ **Resolvido** (19/07/2026) — `StatusConta` ganhou `PARCIAL`, alcançado por
     `ContaPagarReceber#liquidarParcial(valorPagoAdicional, dataPagamento)`: acumula pagamento
     parcial num novo campo `valorPago`, vira liquidação completa (`PAGO`/`RECEBIDO`) quando o
     acumulado cobre o valor total. Endpoint `PATCH /admin/financeiro/contas/{id}/liquidar-parcial`,
     tela "Parcial" em Contas (Financeiro). Não gera `LancamentoFinanceiro` automático por
     pagamento parcial (só a liquidação total gera, mesmo comportamento de antes) — decisão de
     escopo, não há ainda modelo de "parcelamento de conta" definido (distinto do
     `ParcelaVenda` do E13, que é de venda, não de conta a pagar/receber). Migration
     `V31__conta_status_parcial.sql`.
  2. ✅ **Resolvido** (19/07/2026) — `OrigemVenda` ganhou `PARCEIRO`, categoria própria e
     diferente de `CORTESIA` (confirmado pelo Marcos: "são categorias diferentes"). Venda direta
     comercial **não** precisou de valor de enum novo — já é `OrigemVenda.DIRETA` +
     `Lead.getVendedor()` (nome da vendedora), confirmado como o mesmo dado só com nomenclatura
     mais genérica na planilha. Migration `V32__origem_venda_parceiro.sql`.
  3. ✅ **Resolvido** (19/07/2026) — `VendaIngresso` ganhou `nomeEmpresa` (texto simples, mesmo
     critério de `Mentorado.nomeFantasia` — não é PII de indivíduo), `telefone` e `email`
     (criptografados via pgcrypto, mesmo critério de `Lead.telefone`/`Lead.email`). Todos
     opcionais — nem toda venda real na planilha tem os 3 preenchidos. Migration
     `V36__venda_ingresso_empresa_contato.sql`.
  4. A planilha "Eventos - Despesas e Receitas" é um consolidado via `IMPORTRANGE` da planilha
     "Vendas Eventos" (ID `1h8BmrO41_pWW7IIfjJ72OxnWGtRasQfEooJt_BV_jsQ`) — **mapeada em
     18/07/2026**, ver seção 2.
  5. ✅ **Resolvido** (19/07/2026) — **"Ficha técnica Lucrativa"** confirmado como categoria
     própria de `ProdutoVenda` (`FICHA_TECNICA_LUCRATIVA`), mesmo nível de Fórmula SAW/Formação
     Profissional — não vira `TipoContrato` (mesmo tratamento dos outros produtos "avulsos").
     Migration `V33__produto_venda_ficha_tecnica_lucrativa.sql`.
  6. ✅ **Resolvido** (19/07/2026) — `FormaPagamento` ganhou `PIX_RECORRENTE`, distinto de `PIX`
     avulso. Ainda não conectado ao cálculo de MRR (H14.3) — só o catálogo foi resolvido nesta
     leva, o dashboard de faturamento continua sem diferenciar as duas formas na composição de
     receita recorrente. Migration `V34__forma_pagamento_pix_recorrente.sql`.
  7. **Ainda não resolvido, fora de escopo desta leva.** Vendas via Hotmart parecem registrar o
     valor líquido (pós-taxa da plataforma) como "Valor pago no ato", não o valor bruto —
     hipótese fundamentada em 11/23 linhas reais de "Vendas Aline Melo" com o mesmo padrão
     (~87-91% do total, restante sempre R$0 mesmo com total ≠ pago). Se confirmado,
     `Lead.fecharVenda()` precisa de um terceiro conceito (taxa/comissão de plataforma retida),
     senão toda venda Hotmart aparenta ficar "devendo" quando na verdade está quitada pelo
     cliente.
  8. ✅ **Resolvido** (19/07/2026) — `CategoriaIngresso` corrigido: `CORTESIA` saiu do enum
     (confirmado sem nenhuma linha em produção usando esse valor, verificado via grep antes da
     migration), `BLACK` entrou como quarta categoria real, tratada como **permanente**
     (confirmado pelo Marcos: "trate o black como permanente"), não como campanha específica de
     um evento só. Enum final: `ESSENCIAL`/`VIP`/`ESPECIAL`/`BLACK`. Migration
     `V35__categoria_ingresso_black.sql`.

## 🔒 Pausado nesta leva (implementado em 17/07/2026)

Confirmado pelo cliente: foco é **Comercial (E13), Financeiro (E14) e Mentorados — gestão pelo
time interno (E11/E17)**. A Loja não é prioridade (confirmado 2x nas respostas acima) e a área de
auto-atendimento do mentorado não deveria nem estar em uso (acesso restrito à equipe interna,
ver "Acesso e Permissões" na ata). Implementado como *feature flag* reversível, sem apagar
código nem dado:

- `frontend/src/shared/lib/featureFlags.ts` — `AREA_MENTORADO_PAUSADA` e `LOJA_ADMIN_PAUSADA`.
  Trocar pra `false` reabre.
- `frontend/src/app/MentoradoShell.tsx` — com a flag ligada, qualquer usuário `MENTORADO` que
  logar vê uma tela "Área temporariamente indisponível" em vez do dashboard/menu — cobre
  `/mentorado/*` inteiro, inclusive `/mentorado/loja`.
- `frontend/src/features/comercial/ComercialShell.tsx` — abas "Loja — Produtos" e
  "Loja — Pedidos" removidas da navegação do Comercial.
- `frontend/src/App.tsx` — rotas `/admin/comercial/produtos` e `/admin/comercial/pedidos`
  mostram a mesma tela de aviso em vez de `ProdutosPage`/`PedidosPage`, caso alguém acesse a URL
  direto.
- Nada no backend mudou — dado, API e RBAC continuam intactos, é bloqueio só de navegação/UI.
- **Fica pendente:** a suíte E2E que cobre a área do mentorado e a Loja (`dashboard-mentorado`,
  `metas`, `tarefas`, `mentorias`, `materiais`, `eventos`, `loja`, `perfil`, `avisos.spec.ts`
  parcialmente) vai começar a falhar contra essas telas pausadas — não foram tocados. Avisar se
  quiser que também sejam marcados como `test.skip` enquanto durar a pausa.

## Registrado, não descartado (fora da Loja/Mentorado pausados)
- Plataforma de aulas gravadas hospedada no próprio sistema — já é backlog conhecido do E6 no
  `CLAUDE.md`; a reunião reforça isso como incentivo de uso contínuo, mas segue sem decisão de
  arquitetura de storage.
- E-mail automático de confirmação de compra de ingresso (hoje é WhatsApp manual + grupo) —
  cliente achou "interessante" mas não confirmou como prioridade.
- Rename "Mentorados" → "Gestão de Performance" (ver E5 acima) — registrado, não implementado.

## Perguntas pendentes pro Victor

As 4 perguntas desta lista foram todas resolvidas pelo Marcos direto, sem precisar perguntar ao
Victor (a 4ª, que veio do raio-x do "CREDENCIAMENTO", foi resolvida em 19/07/2026 — ver abaixo).
Nenhuma pergunta em aberto no momento; a única decisão real ainda pendente de terceiro é o gap 7
(hipótese Hotmart líquido/bruto), que precisa do cliente mesmo — ver "Implicações pro desenho"
abaixo.

1. **"Fórmula SAW"** (aba do CRM Saw) — **resolvido:** é produto vendável à parte, categoria
   própria de `ProdutoVenda` (mesmo nível de Mentoria Contínua/Individual/Consultoria). Não é o
   kit/metodologia ("Fórmula de Sucesso" do `Fluxograma.pdf`) — hipótese original descartada.
   Implementado em `V27__produto_venda_formula_saw.sql`.
2. **"Formação Profissional"** — **resolvido:** também entra como produto à parte, paralelo aos
   demais, não é sinônimo de nenhum já mapeado. Implementado em
   `V30__produto_venda_formacao_profissional.sql`.
3. O campo **`Vencimento`** no card do mentorado (Notion CRM Saw) — **resolvido:** é simples, um
   par de campos diretos — "data de início" e "data de vencimento" — sem regra derivada (não é
   dia do mês nem dias restantes). Import do M24 deve ler os dois literalmente do export do
   Notion, não computar a partir de `TipoContrato.calcularVencimento()` (regra de 12 meses fixos,
   pode divergir de contrato renegociado/estendido na base real). Ainda não implementado — M24
   segue bloqueado esperando o export do Notion, mas a regra de mapeamento já está definida.
4. **`Comum`/`Vip` (planilha "CREDENCIAMENTO") é uma simplificação de
   `Essencial`/`Vip`/`Especial`/`Black` (planilha "Vendas Eventos"), ou uma categorização
   genuinamente diferente?** — achado em 18/07/2026, **resolvido pelo Marcos em 19/07/2026,
   direto (sem precisar do Victor):** são categorias diferentes, `Comum` não é sinônimo/
   simplificação de nenhum dos 4 tipos comerciais — é um rótulo próprio do credenciamento físico,
   sem mapeamento 1:1 forçado pro `CategoriaIngresso` do backend. Sem mudança de código por causa
   disso: não existe hoje um campo "categoria de credenciamento" separado da `CategoriaIngresso`
   comercial, e nada nesta leva pediu criar um. Mesma dúvida sobre `Compra`
   (`HOTMART`/`VENDA DIRETA COMERCIAL`) vs. `Origem da Venda` também resolvida: **venda direta
   comercial é o mesmo dado que nome de vendedora** (já coberto por `OrigemVenda.DIRETA` +
   `Lead.vendedor`, sem precisar de valor de enum novo pra isso), e **`Cortesia`/`Parceiro` são
   categorias diferentes entre si** — `OrigemVenda` ganhou `PARCEIRO` como valor próprio (gap 2,
   `V32__origem_venda_parceiro.sql`). Filosofia de trabalho confirmada pelo Marcos pra essas
   decisões de catálogo daqui pra frente: "vamos sempre trabalhar com gerais e se depois o
   cliente quiser tirar, tiramos" — inclui/generaliza categoria quando o dado real mostra que
   existe, sem esperar confirmação do Victor pra decisões que são só de nomenclatura/catálogo
   (reservado pro Victor só o que é regra de negócio real, ex.: item 3 acima).

## Plano pra migrar o "Diagnóstico Inicial" do Notion (decidido em 17/07/2026)

O único campo sem fonte estruturada em nenhuma planilha é o **"Diagnóstico Inicial"** (faturamento
anual, nº de colaboradores, empresa regularizada, nº de lojas, CMV, tempo médio de atendimento,
cultura construída, processos desenhados) — vive só como texto livre por página no Notion. O
resto do cadastro (contato, produto, valor, forma de pagamento) já tem fonte estruturada
confiável na planilha "Vendas [Vendedora]", não precisa desse tratamento.

**Decidido: sem reentrada manual pelo time.** O plano é: (1) exportar a página "CRM Saw" inteira
do Notion (Markdown & CSV, incluindo subpáginas e conteúdo) — isso baixa um Markdown por
mentorado com o texto do "Diagnóstico Inicial" preservado; (2) escrever um script de parsing que
lê o padrão "Rótulo: valor" (consistente no registro que já vimos, ex. *"Faturamento anual até
dezembro: 600k"*) e popula os campos estruturados automaticamente; (3) registros que não baterem
no padrão esperado (rótulo faltando, formatação diferente) ficam sinalizados pra alguém do time
completar só aquele campo, não o cadastro inteiro. Fica pendente até o export completo (~40
páginas de mentorado) estar disponível — sem bloquear o resto da implementação.

### 🔒 Pendência bloqueada (19/07/2026): Marcos não tem permissão de export no Notion
O Marcos acessa esse workspace como **Guest** (convidado), não como Membro — o menu `···` da
página "CRM Saw" mostra a opção **"Export" desativada** (cinza, não clicável). Diferente do
bloqueio equivalente no Google Sheets (planilha "Somente ver"), aqui **não existe workaround
técnico**: sem ferramenta de automação de navegador nesta sessão, sem sessão logada do Marcos
pra usar (não deve ser compartilhada), URL é `app.notion.com` (autenticada, `WebFetch` não
funciona), e o Notion não tem um endpoint de leitura pública equivalente ao CSV export de link
público do Sheets. Testado (19/07/2026): confirmado que não há alternativa viável do lado do
Marcos.

**Só desbloqueia com ação do Matheus** (dono do workspace, aparece como "Notion de Matheus
Br..." no topo da página) — uma das três:
1. Ele mesmo faz o export (`···` → `Export` → `Markdown & CSV`, com subpáginas) e compartilha o
   arquivo.
2. Promove o Marcos de Guest pra Membro nesse workspace (mesmo que temporário).
3. Cria uma integração do Notion (Settings → Connections) e compartilha "CRM Saw" com ela — só
   vale a pena se isso virar necessidade recorrente, não pra um export único.

**Bloqueia**: a parte do M24 que depende do "Diagnóstico Inicial" (texto livre, só existe no
Notion). O resto do M24 (contato/produto/valor via planilhas) não depende disso.

## Nota de segurança
A transcrição colada nesta conversa trazia credenciais em texto puro (login de demo mentorado e
um login pessoal). Elas **não foram gravadas em nenhum arquivo deste repositório**. Como esse
texto já circulou fora de um cofre de senhas (transcrição de reunião), vale trocar essas senhas
por precaução.

## Sequenciamento sugerido (prazo: até quarta 22/07)
O cliente confirmou a prioridade "comercial, financeiro e mentorado são os três pontos
principais, marketing e conteúdo ficam pra depois" — mesma ordem que o `CLAUDE.md` já usa pro
MVP. Como todo o MVP já está implementado, o trabalho agora é *change request* em módulos em
produção:
1. Confirmar duração/vencimento da Mentoria Individual (12 meses, como a Contínua?) antes de
   mexer no schema de `Plano`→`TipoContrato` — é o item de maior risco de retrabalho.
2. **Comercial (E13):** formulário único de venda + campos condicionais de ingresso + separação
   ingresso/comissão no dashboard — é o que mais desbloqueia o trabalho manual da vendedora hoje.
3. **Financeiro (E14):** filtro mensal + fixo/variável + subcategorias + alimentação automática
   via venda do mentorado — depende parcialmente do item 2 (parcelamento capturado na venda).
4. **Mentorado/E17:** campo Decisões na ata, controle de presença em aula, gamificação/ranking
   com a lista de 4 ferramentas obrigatórias.
5. **Notion → sistema:** aguardar o cliente enviar a lista de recursos antes de migrar Materiais.

## Como passar a estrutura de planilhas/Notion recebida

- **Planilhas (Google Sheets):** exportar cada aba relevante como CSV (`Arquivo → Fazer
  download → Valores separados por vírgula`) — uma aba por arquivo (venda por fora, venda de
  ingresso, credenciamento). CSV é o formato que o import do E13 (M22) já sabe ler; mesmo que o
  layout não bata 100%, ter o CSV real acelera adaptar o parser ao formato de verdade em vez de
  arriscar a partir da descrição verbal da reunião. **Alternativa mais precisa, usada de fato**
  (todas as abas têm fórmula, não só valor estático — e o objetivo real é mapear a estrutura
  relacional entre as abas, não só puxar valor): `docs/raio-x-planilhas.gs` — script Google Apps
  Script somente leitura (todo método usado é getter, nenhum grava na planilha de origem) que
  roda dentro da própria conta do Marcos (sem credencial, sem login automatizado) e exporta, por
  aba: valor + fórmula literal de cada célula, células mescladas, intervalos nomeados, regras de
  validação/dropdown (pega os valores permitidos reais, ex. "Status do Pagamento"/"Tipo de
  ingresso", direto da fonte) e sinaliza toda fórmula que referencia outra aba/planilha
  (`Aba!Célula`, `IMPORTRANGE`) — é essa a parte que revela como os totais dependem dos detalhes
  entre as abas de um mesmo evento (`Total Despesas_N` → `Despesas_N`, etc.), não só o dado.
- **Notion:** cada página/database tem exportação própria (`···` → `Export` → `Markdown & CSV`,
  com opção de incluir subpáginas). Para bases tipo tabela (aba de cada mentorado, controle de
  aula) exportar como CSV; para conteúdo em texto livre (manuais, template de ata) o Markdown
  preserva a estrutura melhor.
- **Onde colocar:** como esses dados são de mentorado (sigilosos, conforme o próprio Victor
  reforçou), não commitar em nenhum arquivo versionado do repositório. Seguindo o mesmo padrão
  já usado pra `transicao-acessos-para-cliente.md`/`custos-operacionais.md` (arquivos locais,
  fora do Git): crie uma pasta local, ex. `./dados-cliente-notion/` na raiz do repo, adicione-a
  no `.gitignore` se ainda não estiver coberta, solte os exports lá, e me avise o caminho — eu
  leio a estrutura pra desenhar o import/mapeamento sem que o dado em si entre no histórico do
  Git.