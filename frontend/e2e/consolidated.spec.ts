import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test('Fundador sees the real seeded mentee progress and ranking', async ({ page }) => {
  await loginAs(page, 'admin@sawhub.com.br');
  // Painel Consolidado não é mais item próprio da sidebar — virou a 1ª aba (index) dentro de
  // Mentorados, ver App.tsx/MentoradosShell.
  await page.getByRole('link', { name: 'Gestão de Performance' }).click();
  await expect(page).toHaveURL(/\/admin\/mentorados\/consolidado$/);

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

  // M23 — donut de distribuição por status, mesma paleta dos KPIs/StatusPill acima.
  const distribuicaoStatus = page.getByTestId('grafico-distribuicao-status');
  await expect(distribuicaoStatus.locator('svg circle, svg path').first()).toBeVisible();
  await expect(distribuicaoStatus.getByText('Em dia')).toBeVisible();
  await expect(distribuicaoStatus.getByText('Em atenção')).toBeVisible();
  await expect(distribuicaoStatus.getByText('Atrasados')).toBeVisible();

  await page.screenshot({ path: 'e2e/screenshots/consolidado.png', fullPage: true });
});

test('M23 — abas filtram a grade de mentorados por status', async ({ page }) => {
  await loginAs(page, 'admin@sawhub.com.br');
  await page.getByRole('link', { name: 'Gestão de Performance' }).click();
  await expect(page).toHaveURL(/\/admin\/mentorados\/consolidado$/);

  const main = page.getByRole('main');
  await expect(main.getByTestId('mentorado-row').first()).toBeVisible();
  const todasAsLinhas = await main.getByTestId('mentorado-row').count();

  await main.getByRole('tab', { name: 'Em dia' }).click();
  await expect(main.getByRole('tab', { name: 'Em dia' })).toHaveAttribute('aria-selected', 'true');
  const linhasFiltradas = await main.getByTestId('mentorado-row').count();
  expect(linhasFiltradas).toBeLessThanOrEqual(todasAsLinhas);

  await main.getByRole('tab', { name: 'Todos' }).click();
  await expect(main.getByRole('tab', { name: 'Todos' })).toHaveAttribute('aria-selected', 'true');
  await expect(main.getByTestId('mentorado-row')).toHaveCount(todasAsLinhas);
});

// E17 (achado na auditoria do change request 17/07/2026, "dashboard consolidado com filtro por
// mentorado" — não existia, achado confirmado e fechado em 19/07/2026).
test('busca por nome filtra a grade de mentorados, client-side', async ({ page }) => {
  await loginAs(page, 'admin@sawhub.com.br');
  await page.getByRole('link', { name: 'Gestão de Performance' }).click();
  await expect(page).toHaveURL(/\/admin\/mentorados\/consolidado$/);

  const main = page.getByRole('main');
  await expect(main.getByTestId('mentorado-row').first()).toBeVisible();
  const todasAsLinhas = await main.getByTestId('mentorado-row').count();

  await main.getByLabel('Buscar mentorado por nome').fill('João Silva');
  await expect(main.getByTestId('mentorado-row')).toHaveCount(1);
  await expect(main.getByTestId('mentorado-row').getByText('João Silva')).toBeVisible();

  await main.getByLabel('Buscar mentorado por nome').fill('nome que não existe zzz');
  await expect(main.getByText('Nenhum mentorado encontrado com esse nome.')).toBeVisible();

  await main.getByLabel('Buscar mentorado por nome').fill('');
  await expect(main.getByTestId('mentorado-row')).toHaveCount(todasAsLinhas);
});
