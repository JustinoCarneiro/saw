import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M23 — import/export CSV de Conteúdos, sem cobertura E2E até aqui (mesma lacuna que causou o 500
// real em Metas/Tarefas, ver mentorados-comercial-import-export.spec.ts). Import cria como
// rascunho e é tudo-ou-nada (ver Blueprint, ROADMAP.md).
test.describe('Conteúdos — Import/Export CSV (M23)', () => {
  test('exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/lista');

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('conteudos.csv');
  });

  test('importar CSV válido cria o conteúdo como rascunho e ele aparece na listagem', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/lista');

    const titulo = `Conteúdo CSV E2E ${Date.now()}`;
    const csv = `titulo;tipo;url;duracaoMinutos\n`
        + `${titulo};DOCUMENTO;https://cdn.sawhub.com.br/e2e-material.pdf;\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'conteudos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linha = main.locator('text=' + titulo).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Rascunho')).toBeVisible();
  });

  test('importar CSV com URL inválida não cria nada e mostra o erro por linha', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/lista');

    const tituloValido = `Nao deveria entrar ${Date.now()}`;
    const csv = `titulo;tipo;url;duracaoMinutos\n`
        + `${tituloValido};DOCUMENTO;https://cdn.sawhub.com.br/valido.pdf;\n`
        + `Linha ruim;DOCUMENTO;ftp://link-sem-http;\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'conteudos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-erros')).toContainText('Linha 3');
    // Tudo-ou-nada: nem a linha válida do mesmo arquivo entrou.
    await expect(main.getByText(tituloValido)).not.toBeVisible();
  });
});
