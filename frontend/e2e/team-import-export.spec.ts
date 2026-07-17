import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M23 — import/export CSV de Colaboradores (Time), sem cobertura E2E até aqui (mesma lacuna que
// causou o 500 real em Metas/Tarefas, ver mentorados-comercial-import-export.spec.ts). Import CRIA
// colaboradores novos (gera Usuario com a senha informada no CSV) e é tudo-ou-nada (ver Blueprint,
// ROADMAP.md).
test.describe('Time — Import/Export CSV (M23)', () => {
  test('exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Time' }).click();
    await expect(page).toHaveURL(/\/admin\/time$/);

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('colaboradores.csv');
  });

  test('importar CSV válido cria o colaborador e ele aparece na listagem', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Time' }).click();
    await expect(page).toHaveURL(/\/admin\/time$/);

    const nome = `Colaborador CSV E2E ${Date.now()}`;
    const email = `colaborador.csv.e2e.${Date.now()}@sawhub.com.br`;
    const csv = `nome;email;senha;area\n`
        + `${nome};${email};senha12345;MARKETING\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'colaboradores.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linha = main.locator('[data-testid^="colaborador-row-"]', { hasText: nome });
    await expect(linha).toBeVisible();
    await expect(linha.getByText('Marketing')).toBeVisible();
  });

  test('importar CSV com senha curta não cria nada e mostra o erro por linha', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await page.getByRole('link', { name: 'Time' }).click();
    await expect(page).toHaveURL(/\/admin\/time$/);

    const nomeValido = `Nao deveria entrar ${Date.now()}`;
    const emailValido = `nao.deveria.${Date.now()}@sawhub.com.br`;
    const csv = `nome;email;senha;area\n`
        + `${nomeValido};${emailValido};senha12345;MARKETING\n`
        + `Linha ruim;linha.ruim.${Date.now()}@sawhub.com.br;curta;MARKETING\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'colaboradores.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-erros')).toContainText('Linha 3');
    // Tudo-ou-nada: nem a linha válida do mesmo arquivo entrou.
    await expect(main.getByText(nomeValido)).not.toBeVisible();
  });
});
