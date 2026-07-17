import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M23 — import/export CSV de Avisos, sem cobertura E2E até aqui (mesma lacuna que causou o 500
// real em Metas/Tarefas, ver mentorados-comercial-import-export.spec.ts). Import cria = publica
// direto (sem rascunho, M17) e é tudo-ou-nada (ver Blueprint, ROADMAP.md).
test.describe('Avisos — Import/Export CSV (M23)', () => {
  test('exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/avisos');

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('avisos.csv');
  });

  test('importar CSV válido publica o aviso e ele aparece na listagem', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/avisos');

    const titulo = `Aviso CSV E2E ${Date.now()}`;
    const csv = `titulo;descricao;categoria;planoMinimo\n`
        + `${titulo};Publicado via import CSV E2E.;GERAL;GRATUITO\n`;

    await page.getByTestId('csv-importar-input').setInputFiles({
      name: 'avisos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(page.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    await expect(page.locator('[data-testid^="aviso-row-"]', { hasText: titulo })).toBeVisible();
  });

  test('importar CSV com categoria inválida não publica nada e mostra o erro por linha', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/avisos');

    const tituloValido = `Nao deveria publicar ${Date.now()}`;
    const csv = `titulo;descricao;categoria;planoMinimo\n`
        + `${tituloValido};Descrição válida.;GERAL;GRATUITO\n`
        + `Linha ruim;Descrição.;CATEGORIA_FALSA;GRATUITO\n`;

    await page.getByTestId('csv-importar-input').setInputFiles({
      name: 'avisos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(page.getByTestId('csv-import-erros')).toContainText('Linha 3');
    // Tudo-ou-nada: nem a linha válida do mesmo arquivo entrou.
    await expect(page.getByText(tituloValido)).not.toBeVisible();
  });
});
