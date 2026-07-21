import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M21 — import/export CSV do Financeiro. Import é tudo-ou-nada (ver Blueprint, ROADMAP.md): uma
// linha inválida rejeita o arquivo inteiro, nada é persistido.
test.describe('Financeiro — Import/Export CSV (M21)', () => {
  test('Lançamentos: exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/lancamentos$/);

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('lancamentos.csv');
  });

  test('Lançamentos: importar CSV válido cria as linhas e mostra a confirmação', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();

    const descricao = `Import CSV E2E ${Date.now()}`;
    // M26 — cabeçalho ganhou dataVencimento (opcional); "Assinaturas" virou "Mentoria Contínua"
    // (V40 renomeia a categoria órfã da Loja/E8 pausada) — ver ROADMAP.md § "Blueprint (M26)".
    const csv = `tipo;categoria;descricao;valor;dataCompetencia;dataVencimento;status\n`
        + `RECEITA;Mentoria Contínua;${descricao};250,00;15/07/2026;;REALIZADO\n`;

    await page.getByTestId('csv-importar-input').setInputFiles({
      name: 'lancamentos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    const main = page.getByRole('main');
    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    await expect(main.getByText(descricao)).toBeVisible();
  });

  test('Lançamentos: importar CSV com uma linha inválida não cria nada e mostra o erro por linha', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();

    const descricaoValida = `Nao deveria entrar ${Date.now()}`;
    const csv = `tipo;categoria;descricao;valor;dataCompetencia;dataVencimento;status\n`
        + `RECEITA;Mentoria Contínua;${descricaoValida};100,00;15/07/2026;;REALIZADO\n`
        + `RECEITA;CategoriaFalsa;Linha ruim;50,00;16/07/2026;;REALIZADO\n`;

    await page.getByTestId('csv-importar-input').setInputFiles({
      name: 'lancamentos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    const main = page.getByRole('main');
    await expect(main.getByTestId('csv-import-erros')).toContainText('Linha 3');
    await expect(main.getByTestId('csv-import-erros')).toContainText('CategoriaFalsa');
    // Tudo-ou-nada: nem a linha válida do mesmo arquivo entrou.
    await expect(main.getByText(descricaoValida)).not.toBeVisible();
  });

  // Change request 20/07/2026 — "Contas a pagar/receber" fundida em "Lançamentos" (mesma tabela
  // desde o M26); export de "Contas" foi removido junto com a aba (redundante com o de
  // Lançamentos, mesmo CSV). Este teste preserva o cenário que só "Contas" cobria: importar uma
  // linha PREVISTO com dataVencimento preenchida.
  test('Lançamentos: importar CSV com status Previsto e dataVencimento cria a linha', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Lançamentos' }).click();

    const descricao = `Previsto import CSV E2E ${Date.now()}`;
    const csv = `tipo;categoria;descricao;valor;dataCompetencia;dataVencimento;status\n`
        + `DESPESA;Infraestrutura;${descricao};88,00;25/07/2026;25/07/2026;PREVISTO\n`;

    await page.getByTestId('csv-importar-input').setInputFiles({
      name: 'lancamentos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    const main = page.getByRole('main');
    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    await expect(main.getByText(descricao)).toBeVisible();
  });
});
