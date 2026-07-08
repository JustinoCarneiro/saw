import type { Page } from '@playwright/test';

export const SEEDED_PASSWORD = 'trocar-no-primeiro-login';

export async function loginAs(page: Page, email: string, password = SEEDED_PASSWORD) {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
}
