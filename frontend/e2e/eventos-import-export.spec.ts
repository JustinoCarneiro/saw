import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M23 — import/export CSV de Eventos, sem cobertura E2E até aqui (mesma lacuna que causou o 500
// real em Metas/Tarefas, ver mentorados-comercial-import-export.spec.ts). Import cria como
// PROGRAMADO e é tudo-ou-nada (ver Blueprint, ROADMAP.md).
test.describe('Eventos — Import/Export CSV (M23)', () => {
  test('exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/eventos');

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('eventos.csv');
  });

  test('importar CSV válido cria o evento como Programado e ele aparece na listagem', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/eventos');

    const titulo = `Evento CSV E2E ${Date.now()}`;
    const csv = `titulo;tipo;tema;dataHora;local;linkOnline;vagas\n`
        + `${titulo};AO_VIVO;Tema de teste;31/12/2026 10:00;;https://meet.google.com/e2e;20\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'eventos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linha = main.locator('text=' + titulo).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Programado')).toBeVisible();
  });

  test('importar CSV com data/hora inválida não cria nada e mostra o erro por linha', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/eventos');

    const tituloValido = `Nao deveria entrar ${Date.now()}`;
    const csv = `titulo;tipo;tema;dataHora;local;linkOnline;vagas\n`
        + `${tituloValido};AO_VIVO;;31/12/2026 10:00;;https://meet.google.com/e2e;\n`
        + `Linha ruim;AO_VIVO;;31-12-2026;;https://meet.google.com/e2e;\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'eventos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-erros')).toContainText('Linha 3');
    // Tudo-ou-nada: nem a linha válida do mesmo arquivo entrou.
    await expect(main.getByText(tituloValido)).not.toBeVisible();
  });
});
