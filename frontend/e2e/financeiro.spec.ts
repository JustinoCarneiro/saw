import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('Financeiro (E14)', () => {
  test('Fundador vê o dashboard de faturamento com dados reais', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/dashboard$/);

    const main = page.getByRole('main');
    await expect(main.getByText('Faturamento do mês')).toBeVisible();
    await expect(main.getByText('Receita recorrente (MRR)')).toBeVisible();
    await expect(main.getByText('Composição da receita')).toBeVisible();
    await expect(main.getByText('Assinaturas (recorrência)')).toBeVisible();
  });

  test('DRE mostra a hierarquia estruturada com resultado calculado', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'DRE' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/dre$/);

    const main = page.getByRole('main');
    await expect(main.getByText('Receita Bruta').first()).toBeVisible();
    await expect(main.getByText(/Demonstrativo Estruturado/)).toBeVisible();
    await expect(main.getByText('Receita Líquida')).toBeVisible();
    await expect(main.getByText('Comparativo com o mês anterior')).toBeVisible();
  });

  test('Lançamentos lista o seed real e permite criar um novo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/lancamentos$/);

    const main = page.getByRole('main');
    await expect(main.getByText('Assinaturas do mês').first()).toBeVisible();

    const descricao = `Lançamento de teste E2E ${Date.now()}`;
    await main.getByRole('button', { name: 'Novo lançamento' }).click();
    await main.getByLabel('Categoria').selectOption({ label: 'Loja SAW' });
    await main.getByLabel('Descrição').fill(descricao);
    await main.getByLabel('Valor (R$)').fill('123.45');
    await main.getByRole('button', { name: 'Salvar lançamento' }).click();

    await expect(main.getByText(descricao)).toBeVisible();
  });

  // Fase 5 (H14.1) — antes desta feature, uma instalação sem SEED_DEMO_DATA=true não tinha
  // categoria nenhuma pra lançar nada e o Admin não tinha como criar uma (achado relatado pelo
  // cliente durante o teste do sistema em produção: dropdown de categoria vazio).
  test('Nova categoria criada aparece imediatamente no dropdown de lançamento', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/lancamentos$/);

    const nomeCategoria = `Categoria E2E ${Date.now()}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Nova categoria' }).click();
    await main.getByLabel('Nome').fill(nomeCategoria);
    await main.getByLabel('Tipo').selectOption({ label: 'Despesa' });
    await main.getByLabel('Grupo DRE').selectOption({ label: 'Despesa Operacional' });
    await main.getByRole('button', { name: 'Salvar categoria' }).click();

    // Some o form de criação e a categoria já está disponível pro form de lançamento, sem reload.
    await expect(main.getByRole('button', { name: 'Salvar categoria' })).toHaveCount(0);
    await main.getByRole('button', { name: 'Novo lançamento' }).click();
    await main.getByLabel('Tipo').selectOption({ label: 'Despesa' });
    await expect(main.getByLabel('Categoria').locator(`option:has-text("${nomeCategoria}")`)).toHaveCount(1);
  });

  test('Contas a pagar/receber permite criar e liquidar uma conta', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Contas a pagar/receber' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/contas$/);

    // Descrição única por execução: a suíte não reseta o banco entre execuções, então uma
    // descrição fixa colide com a linha deixada por uma execução anterior (inclusive uma que
    // falhou no meio do caminho) e quebra o locator abaixo (2 linhas == 2 botões "Liquidar" ==
    // strict-mode violation no Playwright).
    const descricao = `Conta de teste E2E ${Date.now()}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Nova conta' }).click();
    await main.getByLabel('Descrição').fill(descricao);
    await main.getByLabel('Valor (R$)').fill('77.00');
    // M26 — categoria é obrigatória em toda conta/lançamento agora ("todas as vendas e valores
    // precisam ser mapeados no DRE"), não é mais "(opcional)" — ver ROADMAP.md § "Blueprint (M26)".
    await main.getByLabel('Categoria').selectOption({ label: 'Infraestrutura' });
    await main.getByRole('button', { name: 'Salvar conta' }).click();

    const linha = main.locator('text=' + descricao).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Pendente')).toBeVisible();

    await linha.getByRole('button', { name: 'Liquidar' }).click();
    await expect(page.getByText(`Liquidar: ${descricao}`)).toBeVisible();
    await page.getByRole('button', { name: 'Confirmar liquidação' }).click();

    await expect(page.getByText(`Liquidar: ${descricao}`)).not.toBeVisible();
    await expect(linha.getByText('Pago')).toBeVisible();
  });
});
