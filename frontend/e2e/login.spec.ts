import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('Login', () => {
  test('renders the login form', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByAltText(/SAW/)).toBeVisible();
    await expect(page.getByLabel('E-mail')).toBeVisible();
    await expect(page.getByLabel('Senha', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/login.png' });
  });

  test('shows an error for invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('E-mail').fill('matheus@sawhub.com.br');
    await page.getByLabel('Senha', { exact: true }).fill('senha-errada');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByText('E-mail ou senha inválidos.')).toBeVisible();
    // Não deve ter navegado pra área admin.
    await expect(page).toHaveURL(/\/login$/);
  });

  test('logs in as Fundador and the sidebar shows every module', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);

    // Fundador tem acesso irrestrito (E15) — os 6 itens de navegação devem aparecer na sidebar.
    // Painel Consolidado não é mais um item próprio: virou a 1ª aba dentro de Mentorados.
    for (const modulo of ['Dashboard', 'Comercial', 'Financeiro', 'Gestão de Performance', 'Time', 'Conteúdos']) {
      await expect(page.getByRole('link', { name: modulo })).toBeVisible();
    }

    await page.screenshot({ path: 'e2e/screenshots/fundador-landing.png' });
  });
});
