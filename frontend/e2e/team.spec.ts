import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test('Fundador sees the real seeded team and permission matrix', async ({ page }) => {
  await loginAs(page, 'matheus@sawhub.com.br');
  await page.getByRole('link', { name: 'Time' }).click();
  await expect(page).toHaveURL(/\/admin\/time$/);

  const main = page.getByRole('main');
  await expect(main.getByText('Gestão de Time')).toBeVisible();

  // Colaboradores seedados de verdade (DemoDataSeeder), não mock.
  for (const nome of ['Lucas Alves', 'Paula Mendes', 'Ricardo Costa', 'Juliana Lima']) {
    await expect(main.getByText(nome).first()).toBeVisible();
  }

  // Matriz de permissões por área.
  await expect(main.getByText('Matriz de permissões por área')).toBeVisible();
  await expect(main.getByText('Fundador').first()).toBeVisible();

  await page.screenshot({ path: 'e2e/screenshots/team.png', fullPage: true });
});

test('M19 — H15.1: Fundador cadastra um novo colaborador com área definida', async ({ page }) => {
  await loginAs(page, 'matheus@sawhub.com.br');
  await page.getByRole('link', { name: 'Time' }).click();
  await expect(page).toHaveURL(/\/admin\/time$/);

  const nome = `Colaborador E2E ${Date.now()}`;
  const email = `colaborador.e2e.${Date.now()}@sawhub.com.br`;
  await page.getByTestId('novo-colaborador-botao').click();
  await page.getByLabel('Nome').fill(nome);
  await page.getByLabel('Área').selectOption('MARKETING');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel(/Senha/).fill('senha12345');
  await page.getByRole('button', { name: 'Salvar' }).click();

  const linha = page.locator('[data-testid^="colaborador-row-"]', { hasText: nome });
  await expect(linha).toBeVisible();
  await expect(linha.getByText('Marketing')).toBeVisible();
});
