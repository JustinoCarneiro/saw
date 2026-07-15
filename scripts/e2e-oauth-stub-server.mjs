// Stub local de um IdP OAuth2 (papel do Google em produção), só pro E2E (ver e2e-up.sh).
// Sem dependências além do Node. Implementa o mínimo que o Spring Security OAuth2 Client
// precisa pra completar o "authorization code" flow de ponta a ponta: /authorize (auto-aprova,
// sem tela de consentimento — não estamos testando a UI do Google, e sim o que o SAW HUB faz
// com a resposta), /token e /userinfo.
//
// O e-mail/verificação devolvidos por /userinfo são configuráveis em runtime via POST /_config,
// pra um mesmo processo servir tanto o caso de sucesso (e-mail com conta no SAW HUB) quanto os
// de falha (e-mail não verificado, e-mail sem conta) sem precisar reiniciar o stub entre testes.
//
// Uso: node scripts/e2e-oauth-stub-server.mjs [porta=8092]

import { createServer } from 'node:http';
import { URL } from 'node:url';

const porta = Number(process.argv[2] ?? 8092);
const CODE = 'stub-authorization-code';
const ACCESS_TOKEN = 'stub-access-token';

let config = { email: 'matheus@sawhub.com.br', emailVerified: true };

function lerCorpo(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
  });
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${porta}`);

  if (req.method === 'POST' && url.pathname === '/_config') {
    const corpo = JSON.parse(await lerCorpo(req));
    config = { ...config, ...corpo };
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(config));
    return;
  }

  // Simula o consentimento do usuário: aprova na hora e volta pro redirect_uri com o code.
  if (req.method === 'GET' && url.pathname === '/authorize') {
    const redirectUri = url.searchParams.get('redirect_uri');
    const state = url.searchParams.get('state');
    const destino = new URL(redirectUri);
    destino.searchParams.set('code', CODE);
    destino.searchParams.set('state', state);
    res.writeHead(302, { Location: destino.toString() });
    res.end();
    return;
  }

  if (req.method === 'POST' && url.pathname === '/token') {
    await lerCorpo(req); // troca o code pelo token — não precisa validar o code num stub
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ access_token: ACCESS_TOKEN, token_type: 'Bearer', expires_in: 3600 }));
    return;
  }

  if (req.method === 'GET' && url.pathname === '/userinfo') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      sub: config.email,
      email: config.email,
      email_verified: config.emailVerified,
      name: 'Usuário E2E',
    }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: `stub não implementa ${req.method} ${url.pathname}` }));
});

server.listen(porta, () => {
  console.log(`Stub de IdP OAuth2 escutando em http://localhost:${porta}`);
});
