import { expect, test } from '@playwright/test';

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
});
