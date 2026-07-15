// Helpers pro E2E de checkout (E8/M14): registra um pagamento no stub do Mercado Pago (ver
// scripts/e2e-mercadopago-stub-server.mjs) e computa a assinatura HMAC do jeito que o
// MercadoPagoGatewayService.verificarAssinatura espera — mesmo algoritmo documentado
// publicamente pelo gateway, replicado aqui de propósito (não chama o código de produção pra
// gerar o valor esperado, senão um bug no algoritmo passaria despercebido).

import { createHmac } from 'node:crypto';
import type { APIRequestContext } from '@playwright/test';

const MP_STUB_BASE_URL = process.env.MP_STUB_BASE_URL ?? 'http://localhost:8093';
const WEBHOOK_SECRET = process.env.MERCADOPAGO_WEBHOOK_SECRET ?? 'e2e-stub-webhook-secret';

export async function registrarPagamentoNoStub(
  request: APIRequestContext,
  paymentId: string,
  status: string,
  externalReference: string,
) {
  const res = await request.post(`${MP_STUB_BASE_URL}/_config/payments/${paymentId}`, {
    data: { status, external_reference: externalReference },
  });
  if (!res.ok()) throw new Error(`Falha ao registrar pagamento ${paymentId} no stub: ${res.status()}`);
}

export function assinaturaWebhook(dataId: string, xRequestId: string): { xSignature: string; xRequestId: string } {
  const ts = Math.floor(Date.now() / 1000).toString();
  const manifest = `id:${dataId.toLowerCase()};request-id:${xRequestId};ts:${ts};`;
  const v1 = createHmac('sha256', WEBHOOK_SECRET).update(manifest).digest('hex');
  return { xSignature: `ts=${ts},v1=${v1}`, xRequestId };
}
