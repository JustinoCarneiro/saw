import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test('Fundador sees the real seeded team and permission matrix', async ({ page }) => {
  await loginAs(page, 'admin@sawhub.com.br');
  await page.getByRole('link', { name: 'Time' }).click();
  await expect(page).toHaveURL(/\/admin\/time$/);

  const main = page.getByRole('main');
  await expect(main.getByText('Gestão de Time')).toBeVisible();

  // Colaboradores seedados de verdade (DemoDataSeeder), não mock.
  for (const nome of ['Gestão de Performance', 'Comercial', 'Ricardo Costa', 'Juliana Lima']) {
    await expect(main.getByText(nome).first()).toBeVisible();
  }

  // Matriz de permissões por área.
  await expect(main.getByText('Matriz de permissões por área')).toBeVisible();
  await expect(main.getByText('Admin').first()).toBeVisible();

  await page.screenshot({ path: 'e2e/screenshots/team.png', fullPage: true });
});

test('M19 — H15.1: Fundador cadastra um novo colaborador com área definida', async ({ page }) => {
  await loginAs(page, 'admin@sawhub.com.br');
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

// Pedido do Marcos (22/07/2026) — "pagar qualquer colaborador de qualquer área, mais descritivo
// pro financeiro": prova ponta a ponta que "Novo pagamento" em Gestão de Time gera um lançamento
// de verdade no Financeiro, com descrição já nomeando o colaborador e o período (não texto livre
// digitado). "Diretor" é subcategoria de despesa real, já seedada por DemoDataSeeder.
// Observação carrega um marcador único (Date.now()) — a suíte não reseta o banco entre execuções,
// então reruns encontram o lançamento de uma execução anterior com a mesma descrição-base; sem
// isso, o filtro por texto vira strict-mode violation (mesma classe de flake já documentada em
// comercial.spec.ts, "Define meta comercial...").
test('M29 — Novo pagamento em Gestão de Time gera lançamento descritivo no Financeiro', async ({ page }) => {
  await loginAs(page, 'admin@sawhub.com.br');
  await page.getByRole('link', { name: 'Time' }).click();
  await expect(page).toHaveURL(/\/admin\/time$/);

  const marcador = `e2e-${Date.now()}`;
  const main = page.getByRole('main');
  await main.getByTestId('novo-pagamento-botao').click();
  // Escopado pelo testId do form: a página também mostra "Desempenho do Time" com seu próprio
  // PeriodoPicker (Mês/Ano) simultaneamente — getByLabel('Mês') solto bateria nos dois.
  const form = main.getByTestId('pagamento-colaborador-form');
  await form.getByLabel('Colaborador').selectOption({ label: 'Juliana Lima (Marketing)' });
  await form.getByLabel('Tipo de pagamento').selectOption({ label: '13º salário' });
  await form.getByLabel('Subcategoria (Financeiro)').selectOption({ label: 'Diretor' });
  await form.getByLabel('Valor (R$)').fill('1500');
  await form.getByLabel('Mês').selectOption({ label: 'Agosto' });
  await form.getByLabel('Ano').selectOption('2026');
  await form.getByLabel('Observação (opcional)').fill(marcador);
  await form.getByRole('button', { name: 'Registrar pagamento' }).click();
  await expect(main.getByRole('button', { name: 'Registrar pagamento' })).toHaveCount(0);

  await page.goto('/admin/financeiro/lancamentos');
  const financeiro = page.getByRole('main');
  const linha = financeiro.getByTestId('lancamento-row').filter({ hasText: marcador });
  await expect(linha).toBeVisible();
  await expect(linha.getByText('13º salário — Juliana Lima (08/2026)', { exact: false })).toBeVisible();
  await expect(linha.getByText('1.500,00', { exact: false })).toBeVisible();
});

// M20 — H15.6/H15.7: achado da auditoria de cobertura era que carteira/conversaoPct exibidos
// aqui eram dado fixo do seeder, nunca calculado; e não havia visão de desempenho do time nenhuma.
test('M20 — H15.6/H15.7: carteira e desempenho do time são computados, não fixos', async ({ page }) => {
  await loginAs(page, 'admin@sawhub.com.br');
  await page.getByRole('link', { name: 'Time' }).click();
  await expect(page).toHaveURL(/\/admin\/time$/);

  const main = page.getByRole('main');
  // Escopado pelo e-mail (único), não pelo nome "Gestão de Performance" — esse nome é igual ao
  // rótulo da própria área (AreaPill), e Ricardo Costa é outro colaborador da mesma área: hasText
  // por nome bate nas duas linhas (strict-mode violation).
  const linhaGestaoPerf = main.locator('[data-testid^="colaborador-row-"]', { hasText: 'gestao_perf@sawhub.com.br' });
  await expect(linhaGestaoPerf).toBeVisible();
  // Carteira do colaborador "Gestão de Performance" não é mais o valor fixo "38" que o
  // DemoDataSeeder escrevia antes do M20 — outros specs desta suíte criam mentorias reais com ele
  // como mentor, então o valor exato varia conforme a ordem de execução; o que importa é que não
  // é mais aquele fake hardcoded.
  await expect(linhaGestaoPerf.getByText('38')).toHaveCount(0);

  await expect(main.getByText('Desempenho do Time')).toBeVisible();
  await main.getByRole('combobox').first().selectOption('7');

  // "Comercial" já tem MetaComercial seedada pra 2026-07 (meta_fechamentos=5) — comercial.spec.ts
  // fecha leads reais com esse colaborador como vendedor, então fechamentosRealizados/pctAtingido
  // variam com a ordem de execução; a meta em si é fixa.
  const linhaDesempenho = main.locator('[data-testid^="desempenho-row-"]', { hasText: 'Comercial' });
  await expect(linhaDesempenho).toBeVisible();
  // getByText('5') sozinho é ambíguo: Meta e Realizado podem coincidir no mesmo valor
  // dependendo da ordem de execução (ver comentário acima) — escopa pra célula certa.
  await expect(linhaDesempenho.getByTestId('meta-fechamentos')).toHaveText('5');

  await page.screenshot({ path: 'e2e/screenshots/team-desempenho.png', fullPage: true });
});
