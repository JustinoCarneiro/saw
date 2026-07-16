# Custos operacionais do SAW HUB — mapeamento pro cliente

Todos os custos recorrentes de operar o sistema, pra além do que já foi pago (desenvolvimento).
Valores de mercado pesquisados em 16/07/2026 — **marcados onde precisam de confirmação direta
na fonte antes de virar compromisso formal com o cliente**, preços de serviço externo mudam.

## 1. Custos fixos mensais

| Item | Valor | Fonte/observação |
|---|---|---|
| **VPS Hostinger (KVM 2)** | ~R$ 39–43/mês (~R$ 468–516/ano com desconto anual) | Já documentado no CLAUDE.md (R$35-55/mês) — confirmado como preço real do plano KVM 2 (2 vCPU, 8GB RAM, 100GB NVMe, 2TB banda) [[Hostinger]](https://www.hostinger.com/vps-hosting). Com a migração pra VPS dedicada do cliente (ver `transicao-acessos-para-cliente.md`), esse é o custo cheio — não muda por deixar de ser compartilhada. |
| **Domínio .com.br** | R$ 40/ano (~R$ 3,33/mês) | Preço oficial Registro.br, único registrador autorizado — qualquer revendedor cobra em cima disso [[Registro.br]](https://registro.br/dominio/). |
| **E-mail corporativo** (opcional, mas recomendado) | R$ 32–42/usuário/mês | Google Workspace Business Starter — **se o cliente já tiver e-mail próprio (ex.: outro provedor), esse custo pode não existir**. Alternativa: manter e-mail transacional só pro sistema (recuperação de senha, avisos) via serviço mais barato (Amazon SES, Brevo), não precisa ser Workspace completo — decisão em aberto. |

**Subtotal fixo (cenário mínimo, sem Workspace):** ~R$ 42–47/mês.
**Subtotal fixo (com 1 licença Workspace):** ~R$ 75–90/mês.

## 2. Custos variáveis (por uso)

### 2.1 IA — transcrição + rascunho de ata (E5, diferencial validado hoje)

| Serviço | Preço unitário | Fonte |
|---|---|---|
| Whisper (OpenAI, transcrição) | US$ 0,006/minuto de áudio (~US$ 0,36/hora) | [OpenAI/diyai.io](https://diyai.io/ai-tools/speech-to-text/openai-whisper-api-pricing-2026/) |
| Claude Sonnet 5 (Anthropic, rascunho) | US$ 2/milhão tokens de entrada, US$ 10/milhão de saída (preço promocional até 31/08/2026; sobe pra US$ 3/US$ 15 depois) | [Anthropic/pricepertoken.com](https://pricepertoken.com/pricing-page/model/anthropic-claude-sonnet-5) |

**Estimativa na escala do MVP** (10–15 mentorados, ~1 mentoria/mês cada, ~45min de áudio):
- Whisper: ~15 mentorias × 45min × US$0,006/min ≈ **US$ 4/mês** (~R$ 22/mês)
- Claude: cada ata consome ~10-15 mil tokens de entrada (transcrição) + ~800 de saída → ~US$0,03-0,05 por ata × 15 ≈ **US$ 0,50-0,75/mês** (~R$ 3-4/mês)
- **Total IA nessa escala: ~US$ 4,50-5/mês (~R$ 25-28/mês)** — praticamente irrelevante no orçamento até o volume crescer bem além de 15 mentorados/mês.

### 2.2 Mercado Pago — taxa por venda (E8, Loja)

| Meio de pagamento | Taxa |
|---|---|
| Pix | 0,99% |
| Débito | 2,98% |
| Crédito à vista | 3,99% |
| Crédito parcelado (12x) | 4,99% |

Fonte: [Mercado Pago](https://www.mercadopago.com.br/blog/simulador-de-custos-mercado-pago) — **taxa real pode variar conforme o prazo de recebimento escolhido (na hora vs. D+14/D+30); confirmar no simulador oficial deles antes de fechar preço com o cliente.** Não é custo fixo — só incide sobre venda realizada, proporcional ao faturamento da Loja.

### 2.3 Backup off-site (ainda não implementado, pendência já mapeada)

Backblaze B2 (candidato): ~US$ 6/TB/mês de armazenamento. Pro volume atual do SAW HUB (banco + áudios, ordem de poucos GB), fica em **centavos de dólar por mês** — não é um custo que pese na conta.

## 3. Estimativa total mensal — cenário MVP atual (10–15 usuários)

| Cenário | Total aproximado |
|---|---|
| Sem Workspace, sem backup off-site ainda | **~R$ 70–80/mês** |
| Com Workspace (1 licença) | **~R$ 100–120/mês** |

Isso **não inclui** taxa do Mercado Pago (proporcional às vendas da Loja, não é custo fixo) nem vídeo (item de backlog, custo ainda não decidido — ver CLAUDE.md § E6).

## 4. Fora do escopo atual (backlog, custo não mapeado ainda)

- **Upload de vídeo** (E6) — decisão de arquitetura pendente (ver CLAUDE.md), custo depende da opção escolhida (streaming dedicado tipo Bunny/Cloudflare Stream cobra por GB armazenado + entregue; object storage genérico é mais barato mas sem transcodificação).

## Ressalvas importantes

- Preços de serviços externos (Hostinger, Google, Mercado Pago, OpenAI, Anthropic) **mudam sem aviso** — os valores aqui são um retrato de 16/07/2026, não um contrato. Reconfirmar direto na fonte antes de repassar como número fechado pro cliente.
- Os custos de IA escalam com o **volume real de mentorias gravadas**, não com o número de usuários cadastrados — se o cliente usar bem mais que 15 mentorias/mês, o número sobe proporcionalmente (ainda assim, a escala de centavos por ata deixa isso barato até um volume bem alto).
- Taxa do Mercado Pago é a maior variável real do orçamento operacional, mas é proporcional ao faturamento da Loja — não existe se não tiver venda.
