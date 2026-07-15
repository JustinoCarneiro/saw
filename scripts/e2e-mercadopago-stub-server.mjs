// Stub local da API do Mercado Pago (Preferences + Payments), só pro E2E (ver e2e-up.sh).
// Sem dependências além do Node. O webhook em si (POST .../webhooks/mercadopago) é o próprio
// SAW HUB que recebe — o teste E2E simula a notificação do Mercado Pago chamando esse endpoint
// direto, com uma assinatura HMAC de verdade (mesmo segredo configurado aqui e no backend). Este
// stub só cobre o lado "SAW HUB chama o Mercado Pago": criar preferência e consultar pagamento.
//
// Uso: node scripts/e2e-mercadopago-stub-server.mjs [porta=8093]

import { createServer } from 'node:http';
import { URL } from 'node:url';

const porta = Number(process.argv[2] ?? 8093);

// paymentId -> { status, external_reference }, registrado pelo teste via POST /_config/payments/:id
// antes de disparar o webhook — o Mercado Pago real nunca revelaria isso de antemão, mas o SAW
// HUB nunca confia no corpo do webhook mesmo assim (sempre re-consulta aqui, ver
// PedidoPagamentoService.processarNotificacao), então simular a "verdade" desse lado é o que importa.
const pagamentos = new Map();

function lerCorpo(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
  });
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${porta}`);

  if (req.method === 'POST' && url.pathname.startsWith('/_config/payments/')) {
    const paymentId = url.pathname.replace('/_config/payments/', '');
    pagamentos.set(paymentId, JSON.parse(await lerCorpo(req)));
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true }));
    return;
  }

  if (req.method === 'POST' && url.pathname === '/checkout/preferences') {
    await lerCorpo(req);
    const id = `stub-pref-${Date.now()}`;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ id, init_point: `http://stub-checkout.invalid/pay/${id}` }));
    return;
  }

  if (req.method === 'GET' && url.pathname.startsWith('/v1/payments/')) {
    const paymentId = url.pathname.replace('/v1/payments/', '');
    const registrado = pagamentos.get(paymentId);
    if (!registrado) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: `pagamento ${paymentId} não registrado no stub (chame POST /_config/payments/${paymentId} antes)` }));
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: registrado.status, external_reference: registrado.external_reference }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: `stub não implementa ${req.method} ${url.pathname}` }));
});

server.listen(porta, () => {
  console.log(`Stub do Mercado Pago escutando em http://localhost:${porta}`);
});
