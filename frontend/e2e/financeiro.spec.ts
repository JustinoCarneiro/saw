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

  // E14 (última pendência) — raio-x da planilha real: Fixa/Variável é atributo da subcategoria,
  // não do lançamento (ver CategoriaFinanceira.natureza).
  test('Categoria com natureza Fixa aparece na quebra de Despesas fixas x variáveis do DRE', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/lancamentos$/);

    const nomeCategoria = `Aluguel E2E ${Date.now()}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Nova categoria' }).click();
    await main.getByLabel('Nome').fill(nomeCategoria);
    await main.getByLabel('Tipo').selectOption({ label: 'Despesa' });
    await main.getByLabel('Grupo DRE').selectOption({ label: 'Custos' });
    await main.getByLabel('Grupo (opcional)').fill('Estrutura');
    await main.getByLabel('Natureza (opcional)').selectOption({ label: 'Fixa' });
    await main.getByRole('button', { name: 'Salvar categoria' }).click();
    await expect(main.getByRole('button', { name: 'Salvar categoria' })).toHaveCount(0);

    await main.getByRole('button', { name: 'Novo lançamento' }).click();
    await main.getByLabel('Tipo').selectOption({ label: 'Despesa' });
    await main.getByLabel('Categoria').selectOption({ label: nomeCategoria });
    await main.getByLabel('Descrição').fill(`Aluguel escritório E2E ${Date.now()}`);
    await main.getByLabel('Valor (R$)').fill('321.00');
    await main.getByRole('button', { name: 'Salvar lançamento' }).click();

    await page.getByRole('link', { name: 'DRE' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/dre$/);
    await expect(main.getByText('Despesas fixas x variáveis')).toBeVisible();
    await expect(main.getByText('Fixas', { exact: true })).toBeVisible();
  });

  // Change request 20/07/2026 — "Contas a pagar/receber" fundida em "Lançamentos" (mesma tabela
  // desde o M26, o cliente achou redundante ter 2 abas pro mesmo dado). "Já foi realizado? Não"
  // no form único substitui o antigo botão "Nova conta"/form dedicado.
  test('Lançamentos: criar um previsto e liquidar (fusão da antiga "Contas a pagar/receber")', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/lancamentos$/);

    // Descrição única por execução: a suíte não reseta o banco entre execuções, então uma
    // descrição fixa colide com a linha deixada por uma execução anterior (inclusive uma que
    // falhou no meio do caminho) e quebra o locator abaixo (2 linhas == 2 botões "Liquidar" ==
    // strict-mode violation no Playwright).
    const descricao = `Lancamento pendente teste E2E ${Date.now()}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Novo lançamento' }).click();
    await main.getByLabel('Tipo').selectOption({ label: 'Despesa' });
    // M26 — categoria é obrigatória em todo lançamento agora ("todas as vendas e valores
    // precisam ser mapeados no DRE"), não é mais "(opcional)" — ver ROADMAP.md § "Blueprint (M26)".
    await main.getByLabel('Categoria').selectOption({ label: 'Infraestrutura' });
    await main.getByLabel('Já foi realizado?').selectOption({ label: 'Não (previsto)' });
    await main.getByLabel('Descrição').fill(descricao);
    await main.getByLabel('Valor (R$)').fill('77.00');
    await main.getByRole('button', { name: 'Salvar lançamento' }).click();

    const linha = main.getByTestId('lancamento-row').filter({ hasText: descricao });
    await expect(linha.getByText('Previsto', { exact: true })).toBeVisible();

    await linha.getByRole('button', { name: 'Liquidar' }).click();
    await expect(page.getByText(`Liquidar: ${descricao}`)).toBeVisible();
    await page.getByRole('button', { name: 'Confirmar liquidação' }).click();

    await expect(page.getByText(`Liquidar: ${descricao}`)).not.toBeVisible();
    await expect(linha.getByText('Pago')).toBeVisible();
  });
});
