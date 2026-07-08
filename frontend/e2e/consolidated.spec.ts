import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test('Fundador sees the real seeded mentee progress and ranking', async ({ page }) => {
  await loginAs(page, 'matheus@sawhub.com.br');
  await page.getByRole('link', { name: 'Painel Consolidado' }).click();
  await expect(page).toHaveURL(/\/admin\/consolidado$/);

  const main = page.getByRole('main');
  await expect(main.getByText('Painel Consolidado')).toBeVisible();

  // KPIs agregados.
  await expect(main.getByText('Em dia').first()).toBeVisible();
  await expect(main.getByText('Progresso médio')).toBeVisible();

  // Mentorados seedados de verdade (alguns aparecem 2x: na grade e no ranking lateral).
  for (const nome of ['João Silva', 'Ana Costa', 'Fernanda Lima', 'Marina Souza']) {
    await expect(main.getByText(nome).first()).toBeVisible();
  }

  // Ranking por crescimento de faturamento.
  await expect(main.getByText('Ranking · Crescimento de Faturamento')).toBeVisible();

  await page.screenshot({ path: 'e2e/screenshots/consolidado.png', fullPage: true });
});
