# Transição de acessos — de conta pessoal/Onda para o cliente (SAW)

Mapeamento de tudo que hoje está sob contas pessoais/da Onda e precisa (ou não) migrar
depois da apresentação. Dividido em duas categorias, porque a estratégia é diferente pra
cada uma.

## Categoria A — Custódia do cliente (dinheiro, identidade, dados pessoais)

Essas contas envolvem dinheiro de verdade, identidade legal do negócio ou dados pessoais —
**precisam** ser do cliente, não é opcional nem fica bem "emprestado".

| # | O que | Onde está hoje | Ação necessária |
|---|---|---|---|
| A1 | **Mercado Pago** — aplicação, credenciais, conta recebedora | Sua conta pessoal/Onda | **Recriar do zero** na conta comercial do cliente (não dá pra "transferir" — o dinheiro das vendas reais só cai na conta certa se o app nascer nela) |
| A2 | **Google Cloud / OAuth** — projeto "SAW HUB", Client ID/Secret | Sua conta Google pessoal | **Transferir propriedade** (IAM) — Client ID/Secret continuam os mesmos, sem precisar redeploy |
| A3 | **Domínio real** (quando escolhido) | Ainda não existe (`sslip.io` é temporário) | Registrar **direto no nome/conta do cliente** desde o início — nunca no seu nome |
| A4 | **Conta Fundador** (login do próprio SAW HUB) | `justinocarneiro161@gmail.com` (seu e-mail pessoal) | Trocar e-mail pro do founder real do cliente (Matheus) + senha nova, só ele sabe |
| A5 | **Remetente de e-mail (SMTP)** — quem envia recuperação de senha, avisos | `justinocarneiro161@gmail.com` (seu Gmail pessoal) | Trocar pra endereço do domínio do cliente (ex.: `contato@sawhub.com.br`) |
| A6 | **VPS Hostinger** — servidor onde tudo roda | **Decisão revista** (2026-07-16): antes estava planejado ficar com a Onda, mas foi definido que a **propriedade da hospedagem também precisa ser do cliente**. **Atenção:** a VPS atual é *compartilhada* com outros projetos da Onda (Sistema Melvin, Sistema Lucas rodam nela junto) — não dá pra transferir titularidade de uma VPS compartilhada. **Migrar o SAW HUB pra uma VPS Hostinger nova, dedicada, registrada na conta/nome do cliente.** A Onda continua operando (SSH, Coolify) — só a titularidade da conta/contrato Hostinger muda. |
| A7 | **OpenAI (Whisper) e Anthropic (Claude)** — chaves de API do diferencial de IA (E5, transcrição + rascunho de ata) | Conta **"Onda"** na OpenAI (confirmado 2026-07-16: free trial esgotado, $0 de crédito, sem cartão cadastrado — feature ficou fora do ar até adicionar pagamento). Conta da Anthropic não conferida ainda, mas é o mesmo padrão. | É custo recorrente por uso (cobra por minuto de áudio transcrito + tokens do rascunho) — cliente deveria bancar o próprio uso a partir de algum ponto, não a Onda pagando indefinidamente. Criar as chaves em contas/organizações do próprio cliente (ou pelo menos com cartão/billing do cliente) e trocar `OPENAI_API_KEY`/`ANTHROPIC_API_KEY` no Coolify. |

## Categoria B — Fica com a Onda (hospedagem gerenciada, só o que resta depois de A6)

Essas são infraestrutura operacional — o próprio CLAUDE.md já documenta que a Onda instala
e cuida de Postgres/Redis/TLS/backups como parte do modelo de hospedagem. A titularidade da
VPS agora é do cliente (A6), mas a Onda continua sendo quem opera no dia a dia — isso não
muda.

| # | O que | Onde está hoje | Ação necessária |
|---|---|---|---|
| B1 | **Repositório GitHub** | Sua conta pessoal (`JustinoCarneiro/saw`) | Mover pra uma **organização da Onda** (não do cliente) — só tira do seu GitHub pessoal |

---

## Passo a passo, na ordem certa

A ordem importa porque algumas coisas dependem de outras (ex.: não dá pra configurar
webhook do Mercado Pago com URL definitiva antes do domínio existir).

### Fase 1 — Pode fazer a qualquer momento, independente de domínio

1. **Google Cloud (A2):** no [Google Cloud Console](https://console.cloud.google.com) →
   IAM e administrador → adiciona o Google Workspace/conta do cliente como **Proprietário**
   do projeto "SAW HUB". O cliente precisa aceitar o convite. Só depois disso, você se
   remove como proprietário (mantém como colaborador se quiser continuar dando suporte).
   Client ID/Secret não mudam — zero impacto no backend.
2. **GitHub (B2):** cria (ou usa, se já existir) uma organização GitHub da Onda, transfere
   o repositório `JustinoCarneiro/saw` pra lá (GitHub tem transferência nativa, preserva
   histórico/issues). Depois, reconecta o Coolify à nova localização do repo (ele usa uma
   chave de deploy ou GitHub App vinculada ao repositório antigo — precisa reautorizar).

### Fase 1.5 — Migração de VPS (A6) — a mais delicada, planejar com calma

**Não fazer isso agora, enquanto o chamado do Mercado Pago (WCS-43120) ainda referencia a
URL/IP atual (`157.173.212.76.sslip.io`).** Migrar de VPS no meio dessa investigação
complica o rastro pro suporte do MP. Fazer só depois do webhook resolver, e idealmente
junto com a Fase 2 (domínio real) — assim o DNS já nasce apontando pro IP definitivo, sem
precisar trocar duas vezes.

Passo a passo:
1. Cliente cria conta Hostinger própria (ou você provisiona já no nome/CNPJ dele) e contrata
   uma VPS nova (mesmo plano KVM 2 ou equivalente).
2. Instala Coolify do zero na VPS nova.
3. Recria os recursos no Coolify novo: aplicações backend/frontend (aponta pro mesmo
   repositório GitHub), banco Postgres, Redis, variáveis de ambiente (copiar os valores
   atuais, não reinventar).
4. Restaura o backup mais recente do Postgres (`backup_saw_hub_postgres.sh`) na VPS nova —
   valida que os dados batem antes de considerar migrado.
5. Configura Nginx do host + TLS (Certbot) na VPS nova.
6. Só troca DNS/domínio pra apontar pra VPS nova depois de confirmar que tudo funciona nela
   em paralelo (pode testar via IP direto ou um `sslip.io` novo antes do cutover final).
7. Depois do cutover confirmado, desliga a instância antiga (ou pelo menos remove o SAW HUB
   dela, já que ela continua servindo Melvin/Lucas).

### Fase 2 — Depois que o cliente decidir e registrar o domínio real (idealmente junto da Fase 1.5)

3. Atualiza no Coolify: `CORS_ALLOWED_ORIGINS`, `EMAIL_FRONTEND_BASE_URL`,
   `LOJA_FRONTEND_BASE_URL`, `LOJA_BACKEND_BASE_URL` pro novo domínio.
4. Nginx do host: novo `server_name`, novo certificado TLS via Certbot pro domínio real
   (mantém `sslip.io` funcionando em paralelo até confirmar que o novo domínio está 100%).
5. Google OAuth: adiciona o novo domínio nas **URIs de redirecionamento autorizadas** —
   pode manter a URI do `sslip.io` até ter certeza que não precisa mais dela.
6. SMTP (A5): assim que o cliente tiver e-mail próprio no domínio (Google Workspace, Zoho,
   etc.), troca `MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_REMETENTE` — melhora inclusive a
   entregabilidade (e-mail de sistema saindo de `@gmail.com` pessoal é sinal ruim de spam).

### Fase 3 — Depois da apresentação ao cliente (conforme você mesmo pediu)

7. **Conta Fundador (A4):** troca o e-mail da conta admin de `justinocarneiro161@gmail.com`
   pro e-mail real do Matheus, gera senha nova (ele troca no primeiro login via "esqueci
   minha senha", assim só ele sabe a senha final). Atualiza também
   `BOOTSTRAP_FUNDADOR_EMAIL` no Coolify por higiene (não tem efeito prático — o bootstrap
   já rodou uma vez só — mas evita confusão numa reinstalação futura).

### Fase 4 — Só depois do webhook do Mercado Pago estar resolvido e o domínio estável

8. **Mercado Pago (A1) — a mais trabalhosa:**
   - Cliente cria a aplicação **na própria conta comercial dele** (CNPJ/dados bancários
     reais, é pra onde o dinheiro das vendas vai cair).
   - Gera credenciais de produção nessa conta nova.
   - Configura Webhooks (URL do domínio real definitivo, não mais `sslip.io`).
   - Atualiza `MERCADOPAGO_ACCESS_TOKEN`/`MERCADOPAGO_WEBHOOK_SECRET` no Coolify.
   - **Reteste completo do fluxo de compra→webhook→liberação** com as credenciais novas —
     não assume que "já validamos isso antes", são credenciais e conta diferentes.

---

## Coisas que NÃO precisam de ação (você já não tem acesso exclusivo)

- **Backup do Postgres** — roda localmente na VPS, é operação da Onda (Categoria B).
- **Redis, Postgres** — infra interna, não exposta a ninguém fora da Onda.

## Risco a evitar

Durante a Fase 1 e 2, **não remova seu próprio acesso de nada até confirmar que o acesso
do cliente/Onda-org está funcionando** — teste o login do cliente como proprietário antes
de se autorremover do Google Cloud, por exemplo. Perder acesso de administrador de um
projeto Google sem ter confirmado que o novo dono realmente consegue operar é o tipo de
erro que trava tudo até abrir chamado de suporte com o próprio Google.
