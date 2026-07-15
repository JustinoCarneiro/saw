import { expect, test } from '@playwright/test';
import { SEEDED_PASSWORD, loginAs } from './helpers';
import { esperarLinkNoUltimoEmail } from './mailpit';

const LINK_REDEFINICAO_REGEX = /https?:\/\/\S+\/redefinir-senha\?token=\S+/;

async function pedirTokenPorEmail(page: import('@playwright/test').Page, email: string): Promise<string> {
  await page.request.post('/api/v1/auth/esqueci-senha', { data: { email } });
  const link = await esperarLinkNoUltimoEmail(email, 'Redefinição de senha', LINK_REDEFINICAO_REGEX);
  const token = new URL(link).searchParams.get('token');
  if (!token) throw new Error(`Link de redefinição sem token: ${link}`);
  return token;
}

test.describe('M18 — H1.4 Recuperar senha', () => {
  test('login mostra o link "Esqueci minha senha" e navega pra tela de recuperação', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('link', { name: 'Esqueci minha senha' }).click();
    await expect(page).toHaveURL(/\/esqueci-senha/);
    await expect(page.getByRole('heading', { name: 'Esqueci minha senha' })).toBeVisible();
  });

  test('solicitar recuperação com e-mail existente mostra a mensagem genérica de confirmação', async ({ page }) => {
    await page.goto('/esqueci-senha');
    await page.getByLabel('E-mail').fill('rafael@bistrogomes.com.br');
    await page.getByRole('button', { name: 'Enviar link de redefinição' }).click();

    await expect(page.getByTestId('esqueci-senha-confirmacao')).toBeVisible();
    await expect(page.getByText('Verifique seu e-mail.')).toBeVisible();
  });

  test('solicitar recuperação com e-mail inexistente mostra a MESMA mensagem genérica (H1.1: sem oráculo de enumeração)', async ({ page }) => {
    await page.goto('/esqueci-senha');
    await page.getByLabel('E-mail').fill('esse-email-nao-existe-no-sistema@x.com');
    await page.getByRole('button', { name: 'Enviar link de redefinição' }).click();

    await expect(page.getByTestId('esqueci-senha-confirmacao')).toBeVisible();
    await expect(page.getByText('Verifique seu e-mail.')).toBeVisible();
  });

  test('acessar /redefinir-senha sem token mostra estado de link inválido', async ({ page }) => {
    await page.goto('/redefinir-senha');
    await expect(page.getByTestId('redefinir-senha-sem-token')).toBeVisible();
    await expect(page.getByText('Link inválido.')).toBeVisible();
  });

  test('redefinir com token inválido mostra o erro genérico do backend', async ({ page }) => {
    await page.goto('/redefinir-senha?token=token-que-nao-existe-de-verdade');
    await page.getByLabel('Nova senha', { exact: true }).fill('novaSenha123');
    await page.getByLabel('Confirmar nova senha').fill('novaSenha123');
    await page.getByRole('button', { name: 'Redefinir senha' }).click();

    await expect(page.getByText('Link inválido ou expirado.')).toBeVisible();
  });

  test('senhas que não coincidem são bloqueadas no cliente, sem chamar o backend', async ({ page }) => {
    await page.goto('/redefinir-senha?token=qualquer-token');
    await page.getByLabel('Nova senha', { exact: true }).fill('senhaUm12345');
    await page.getByLabel('Confirmar nova senha').fill('senhaDoisDiferente');

    let chamouBackend = false;
    page.on('request', (req) => {
      if (req.url().includes('/auth/redefinir-senha')) chamouBackend = true;
    });

    await page.getByRole('button', { name: 'Redefinir senha' }).click();
    await expect(page.getByText('As senhas não coincidem.')).toBeVisible();
    expect(chamouBackend).toBe(false);
  });

  // Os testes acima só cobrem estados de erro (token ausente/inválido) porque não têm como obter
  // um token VÁLIDO — ele nunca é persistido em texto puro (só o hash, ver PasswordResetService),
  // só existe no e-mail. Este teste fecha esse buraco lendo o e-mail de verdade via Mailpit (ver
  // scripts/e2e-up.sh) em vez de confiar só no unitário (PasswordResetServiceTest). Usa Rafael
  // (fixture reaproveitado em vários outros specs via loginAs) e restaura a senha original ao
  // final — sem isso, qualquer spec que rodasse depois deste e fizesse loginAs(rafael) quebraria.
  test('caminho feliz completo: token real recebido por e-mail redefine a senha e permite login', async ({ page }) => {
    const email = 'rafael@bistrogomes.com.br';
    const senhaTemporaria = 'senhaTemporariaE2E123';

    // 1. Pede a redefinição pela tela real (não pela API direto) — cobre a UI de ponta a ponta.
    await page.goto('/esqueci-senha');
    await page.getByLabel('E-mail').fill(email);
    await page.getByRole('button', { name: 'Enviar link de redefinição' }).click();
    await expect(page.getByTestId('esqueci-senha-confirmacao')).toBeVisible();

    // 2. Lê o token real do e-mail que o backend realmente enviou via SMTP (Mailpit).
    const link = await esperarLinkNoUltimoEmail(email, 'Redefinição de senha', LINK_REDEFINICAO_REGEX);
    const token = new URL(link).searchParams.get('token');
    expect(token).toBeTruthy();

    // 3. Redefine pela tela real e confirma que o login novo funciona de fato.
    await page.goto(`/redefinir-senha?token=${token}`);
    await page.getByLabel('Nova senha', { exact: true }).fill(senhaTemporaria);
    await page.getByLabel('Confirmar nova senha').fill(senhaTemporaria);
    await page.getByRole('button', { name: 'Redefinir senha' }).click();
    await expect(page.getByTestId('redefinir-senha-confirmacao')).toBeVisible();
    await page.getByRole('link', { name: 'Ir para o login' }).click();
    await expect(page).toHaveURL(/\/login/);

    await loginAs(page, email, senhaTemporaria);
    await expect(page).toHaveURL(/\/mentorado/);

    // 4. Restaura a senha original (novo ciclo completo de token) — mantém o fixture do Rafael
    // utilizável por qualquer spec que rode depois deste, com a senha seedada de sempre.
    const tokenRestauracao = await pedirTokenPorEmail(page, email);
    const resRestauracao = await page.request.post('/api/v1/auth/redefinir-senha', {
      data: { token: tokenRestauracao, novaSenha: SEEDED_PASSWORD },
    });
    expect(resRestauracao.ok()).toBe(true);
  });
});
