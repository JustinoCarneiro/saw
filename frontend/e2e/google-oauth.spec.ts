import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M07 — Google OAuth (fast-follow do E1)', () => {
  test('sem credencial configurada, o botão "Entrar com Google" não aparece e o login por e-mail/senha continua funcionando', async ({ page }) => {
    // Este ambiente de dev não tem GOOGLE_CLIENT_ID/SECRET configurados (ver ROADMAP.md M07) —
    // é exatamente o cenário que valida que a feature opcional não quebra nem vaza um botão morto.
    await page.goto('/login');
    await expect(page.getByRole('link', { name: 'Entrar com Google' })).toHaveCount(0);

    // Regressão: login e-mail/senha não pode ter sido afetado pela nova config condicional do
    // SecurityConfig (achado a se verificar sempre que Auth é tocado, CLAUDE.md § risco alto).
    await loginAs(page, 'matheus@sawhub.com.br');
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
    expect(body).toEqual({ googleEnabled: false });
  });
});
