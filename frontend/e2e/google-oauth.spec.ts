import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// GOOGLE_CLIENT_ID/SECRET (stub) e as 3 URIs apontam pro stub de IdP local (ver
// scripts/e2e-oauth-stub-server.mjs + e2e-up.sh) — diferente de Mercado Pago (que só tem
// coragem de testar "gateway não configurado"), este ambiente exercita o authorization code
// flow real de ponta a ponta. As 4 combinações de e-mail verificado/existente já estão cobertas
// no unitário (GoogleOAuth2UserServiceTest, mocka o delegate) — aqui só prova que a fiação
// inteira (SecurityConfig -> stub -> callback -> sessão) funciona de verdade.
const OAUTH_STUB_BASE_URL = process.env.OAUTH_STUB_BASE_URL ?? 'http://localhost:8092';

async function configurarStub(request: import('@playwright/test').APIRequestContext, config: { email: string; emailVerified: boolean }) {
  const res = await request.post(`${OAUTH_STUB_BASE_URL}/_config`, { data: config });
  expect(res.ok()).toBe(true);
}

test.describe('M07 — Google OAuth (fast-follow do E1)', () => {
  test('com credencial configurada, o botão "Entrar com Google" aparece e o login por e-mail/senha continua funcionando', async ({ page }) => {
    // Regressão: login e-mail/senha não pode ter sido afetado pela config condicional do
    // SecurityConfig (achado a se verificar sempre que Auth é tocado, CLAUDE.md § risco alto).
    await page.goto('/login');
    await expect(page.getByRole('link', { name: 'Entrar com Google' })).toBeVisible();

    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
  });

  test('mensagem de erro OAuth é traduzida a partir do parâmetro ?erroOAuth=', async ({ page }) => {
    // Código e mensagem propositalmente genéricos (achado do revisor-seguranca: um código
    // "conta_nao_encontrada" seria um oráculo de enumeração de contas) — não pode confirmar nem
    // negar que existe conta SAW HUB pra aquele e-mail.
    await page.goto('/login?erroOAuth=login_nao_permitido');
    await expect(page.getByText('Não foi possível entrar com essa conta Google. Tente e-mail e senha ou solicite acesso abaixo.')).toBeVisible();

    await page.goto('/login?erroOAuth=email_nao_verificado');
    await expect(page.getByText(/e-mail do Google não está verificado/)).toBeVisible();

    await page.goto('/login?erroOAuth=algo-inesperado');
    await expect(page.getByText('Não foi possível entrar com o Google agora. Tente novamente.')).toBeVisible();
  });

  test('GET /api/v1/auth/oauth2-config é público e responde sem sessão', async ({ page, request }) => {
    const res = await request.get('/api/v1/auth/oauth2-config');
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body).toEqual({ googleEnabled: true });
  });

  test('caminho feliz completo: login com Google de ponta a ponta autentica um mentorado existente', async ({ page, request }) => {
    // Rafael tem conta no SAW HUB (seed) — o stub devolve o e-mail dele como se fosse o Google
    // confirmando, com email_verified=true.
    await configurarStub(request, { email: 'rafael@bistrogomes.com.br', emailVerified: true });

    await page.goto('/login');
    await page.getByRole('link', { name: 'Entrar com Google' }).click();

    // Browser atravessa de verdade: /oauth2/authorization/google (Spring) -> stub /authorize
    // (auto-aprova) -> stub /token -> stub /userinfo -> GoogleOAuth2UserService vincula pelo
    // e-mail -> sessão autenticada -> OAuth2SuccessHandler redireciona pro app.
    await expect(page).toHaveURL(/\/mentorado/);

    // Confirma que é sessão de verdade, não só a URL — chamada autenticada devolve o próprio Rafael.
    const me = await page.request.get('/api/v1/auth/me');
    expect(me.ok()).toBe(true);
    expect((await me.json()).email).toBe('rafael@bistrogomes.com.br');
  });

  test('e-mail do Google não verificado é rejeitado, mesmo com conta existente no SAW HUB', async ({ page, request }) => {
    await configurarStub(request, { email: 'rafael@bistrogomes.com.br', emailVerified: false });

    await page.goto('/login');
    await page.getByRole('link', { name: 'Entrar com Google' }).click();

    await expect(page).toHaveURL(/\/login\?erroOAuth=email_nao_verificado/);
    await expect(page.getByText(/e-mail do Google não está verificado/)).toBeVisible();

    // Restaura o default pro resto da suíte (outros specs podem reusar o stub sem saber disso).
    await configurarStub(request, { email: 'admin@sawhub.com.br', emailVerified: true });
  });
});
