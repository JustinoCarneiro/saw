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

// M20 — H15.6/H15.7: achado da auditoria de cobertura era que carteira/conversaoPct exibidos
// aqui eram dado fixo do seeder, nunca calculado; e não havia visão de desempenho do time nenhuma.
test('M20 — H15.6/H15.7: carteira e desempenho do time são computados, não fixos', async ({ page }) => {
  await loginAs(page, 'matheus@sawhub.com.br');
  await page.getByRole('link', { name: 'Time' }).click();
  await expect(page).toHaveURL(/\/admin\/time$/);

  const main = page.getByRole('main');
  const linhaLucas = main.locator('[data-testid^="colaborador-row-"]', { hasText: 'Lucas Alves' });
  await expect(linhaLucas).toBeVisible();
  // Carteira de Lucas Alves não é mais o valor fixo "38" que o DemoDataSeeder escrevia antes do
  // M20 — outros specs desta suíte criam mentorias reais com ele como mentor, então o valor exato
  // varia conforme a ordem de execução; o que importa é que não é mais aquele fake hardcoded.
  await expect(linhaLucas.getByText('38')).toHaveCount(0);

  await expect(main.getByText('Desempenho do Time')).toBeVisible();
  await main.getByRole('combobox').first().selectOption('7');

  // Paula Mendes já tem MetaComercial seedada pra 2026-07 (meta_fechamentos=5) — comercial.spec.ts
  // fecha leads reais com ela como vendedora, então fechamentosRealizados/pctAtingido variam com a
  // ordem de execução; a meta em si é fixa.
  const linhaDesempenho = main.locator('[data-testid^="desempenho-row-"]', { hasText: 'Paula Mendes' });
  await expect(linhaDesempenho).toBeVisible();
  await expect(linhaDesempenho.getByText('5', { exact: true })).toBeVisible();

  await page.screenshot({ path: 'e2e/screenshots/team-desempenho.png', fullPage: true });
});
