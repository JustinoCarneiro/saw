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
    const csv = `tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n`
        + `RECEITA;Assinaturas;${descricao};250,00;15/07/2026;REALIZADO;\n`;

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
    const csv = `tipo;categoria;descricao;valor;dataCompetencia;status;planoReferencia\n`
        + `RECEITA;Assinaturas;${descricaoValida};100,00;15/07/2026;REALIZADO;\n`
        + `RECEITA;CategoriaFalsa;Linha ruim;50,00;16/07/2026;REALIZADO;\n`;

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

  test('Contas: exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Contas a pagar/receber' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/contas$/);

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('contas.csv');
  });

  test('Contas: importar CSV válido cria a linha e mostra a confirmação', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Contas a pagar/receber' }).click();

    const descricao = `Conta import CSV E2E ${Date.now()}`;
    const csv = `tipo;descricao;valor;dataVencimento;categoria\n`
        + `A_PAGAR;${descricao};88,00;25/07/2026;Infraestrutura\n`;

    await page.getByTestId('csv-importar-input').setInputFiles({
      name: 'contas.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    const main = page.getByRole('main');
    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    await expect(main.getByText(descricao)).toBeVisible();
  });
});
