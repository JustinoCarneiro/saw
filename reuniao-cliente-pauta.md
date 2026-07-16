# Pauta — reunião de apresentação/entrega do SAW HUB

Resumo pra levar na reunião: o que precisa ser esclarecido, o que o cliente precisa
providenciar (contas/credenciais) e os custos recorrentes envolvidos.

## 1. Escopo de uso inicial — decisão a alinhar com o cliente

**O sistema inteiro já está pronto e testado** — os 17 épicos (back-office: Comercial,
Financeiro, Gestão de Time, Painel Consolidado; e a jornada completa do mentorado:
dashboard, metas, tarefas, mentorias/atas com transcrição por IA, materiais, eventos, loja,
perfil/gamificação). Não é uma limitação técnica.

**Ponto a esclarecer:** o cliente pode escolher começar usando **só o back-office**
(gerenciar comercial, financeiro e time da própria SAW) e **deixar o módulo do mentorado
desativado/não divulgado no início** — os mentorados não precisam ganhar acesso no dia 1.
Se o cliente gostar do fluxo e quiser abrir pros mentorados depois, **o recurso já está
pronto pra ligar**, sem precisar de desenvolvimento novo — é só uma questão de cadastrar
os mentorados reais e liberar o acesso.

Isso é uma decisão de **rollout do negócio**, não uma pergunta técnica — vale perguntar
diretamente: *"vocês querem já abrir o acesso do mentorado pros clientes de vocês, ou
preferem rodar só a parte interna primeiro e abrir depois?"*

## 2. Contas/credenciais que o cliente precisa providenciar

Hoje várias contas usadas pelo sistema estão em nome pessoal do desenvolvedor ou da Onda —
plano de transição completo em `transicao-acessos-para-cliente.md`. Resumo do que o cliente
precisa **ter ou criar**, se ainda não tiver:

| Conta | Pra quê serve | Cliente já tem? |
|---|---|---|
| **Hostinger** | Hospedagem do sistema (VPS própria, dedicada) | A confirmar |
| **Google Cloud / conta Google** | Login "Entrar com Google" dos mentorados/time | A confirmar |
| **Mercado Pago** (conta comercial, com CNPJ/dados bancários) | Receber os pagamentos da Loja SAW — **é pra essa conta que o dinheiro das vendas cai** | A confirmar — crítico, sem isso não tem como vender de verdade |
| **OpenAI** (plataforma de API, não é o ChatGPT normal) | Transcrição do áudio das mentorias | A confirmar |
| **Anthropic** (Claude, plataforma de API) | Geração do rascunho de ata a partir da transcrição | A confirmar |
| **Domínio próprio** (ex.: sawhub.com.br) | Endereço definitivo do sistema (hoje é um link temporário) | A confirmar — **decisão pendente, precisa escolher o nome** |
| **E-mail corporativo** (opcional) | Remetente dos e-mails do sistema (recuperação de senha, avisos) | A confirmar — pode usar o que já tiverem |

Pra cada uma dessas contas OpenAI/Anthropic/Mercado Pago/Google, **é a Onda que cria e
configura tecnicamente** — o cliente só precisa ser o dono da conta (e-mail, cartão de
cobrança, CNPJ), não precisa saber mexer nelas.

## 3. Custos recorrentes — detalhado em `custos-operacionais.md`

Resumo rápido pra levar de cabeça:

- **Hospedagem + domínio (fixo):** ~R$ 42–47/mês
- **E-mail corporativo (opcional, se optarem por Google Workspace):** + ~R$ 32–42/mês por licença
- **IA (transcrição + ata) na escala atual (10-15 mentorados):** ~R$ 25–28/mês — sobe com o volume de mentorias gravadas, mas em ritmo baixo
- **Mercado Pago:** não é custo fixo, é taxa por venda (0,99% Pix até 4,99% crédito parcelado) — só existe se tiver venda na Loja

**Total fixo estimado: ~R$ 70–120/mês**, dependendo se incluem e-mail corporativo ou não.
Valores pesquisados em 16/07/2026 — reconfirmar na fonte antes de fechar como número final.

## 4. Pendências técnicas — comunicar com transparência (não esconder)

- **Loja/pagamento:** o checkout funciona, mas a liberação automática do pedido após
  pagamento está bloqueada por um bug do lado do Mercado Pago (chamado aberto, aguardando
  engenharia deles — não é algo que dependa de nós corrigir). Até resolver, pedidos pagos
  precisam ser liberados manualmente se alguém comprar de verdade.
- **Domínio atual é temporário** (`sslip.io`) — só serve pra essa apresentação.
- **Criptografia de disco da VPS** não está ativa ainda (só a criptografia a nível de banco,
  que já protege os dados sensíveis) — fica pra uma janela de manutenção planejada.
- **Backup só existe dentro da própria VPS** ainda — cópia fora do servidor (off-site) é
  uma decisão de destino que falta tomar (ex.: Backblaze).
- **Upload de vídeo próprio** (querido pelo cliente, mencionado como interesse futuro) está
  mapeado como backlog — arquitetura ainda não decidida, não é MVP.

## 5. Decisões que precisam sair dessa reunião

1. Vão usar o módulo do mentorado já no início, ou só o back-office por enquanto?
2. Nome do domínio definitivo.
3. Quem é o e-mail/pessoa real que vai ser a conta "Fundador" (hoje é um e-mail de teste).
4. Confirmar que o cliente vai providenciar CNPJ/dados bancários pra criar a conta comercial
   do Mercado Pago (sem isso, a Loja não pode ir pra produção de verdade).
5. Se topam o custo recorrente estimado (~R$ 70-120/mês) antes de seguir pra produção plena.
