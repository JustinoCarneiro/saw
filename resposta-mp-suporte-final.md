[ENVIADO — colado no chamado WCS-43120 em 2026-07-16]

Requisição confirmada via Painel de notificações (corpo bate com o recebido):

payment_id: 168160827637 (2026-07-16T03:33:34Z)

Body:

```json
{
  "action": "payment.created",
  "api_version": "v1",
  "data": {
    "id": "168160827637"
  },
  "date_created": "2026-07-16T03:33:34Z",
  "id": 134782274761,
  "live_mode": true,
  "type": "payment",
  "user_id": "3545576574"
}
```

Estado atual: divergência é exclusivamente na validação do header x-signature em eventos reais (simulador valida com a mesma secret). Implementação manual + reprodução em Python + WebhookSignatureValidator.validate() do SDK Java retornam SIGNATURE_MISMATCH em produção.

Pedidos objetivos para engenharia (logs internos de entrega):

1) Para payment_id 168160827637 (e também 168160335229, se necessário), extrair:
- x-signature efetivamente enviado (ts e v1);
- a string canônica (manifest) usada para gerar a assinatura no Mercado Pago;
- o identificador da secret/config de Webhook aplicada (sem expor a secret).

2) Confirmar se existe qualquer diferença no processo de assinatura entre entrega disparada por "Simular" no painel e evento real payment.created em produção, para a mesma app e user_id 3545576574.
