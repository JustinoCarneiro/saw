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
