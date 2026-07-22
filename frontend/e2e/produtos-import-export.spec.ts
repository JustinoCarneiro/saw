import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M23 — import/export CSV de Produtos (Loja), sem cobertura E2E até aqui (mesma lacuna que causou
// o 500 real em Metas/Tarefas, ver mentorados-comercial-import-export.spec.ts). Import cria como
// rascunho (publicado=false) e é tudo-ou-nada (ver Blueprint, ROADMAP.md).
test.describe('Produtos (Loja) — Import/Export CSV (M23)', () => {
  test('exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/produtos');

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('produtos.csv');
  });

  test('importar CSV válido cria o produto como rascunho e ele aparece na listagem', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/produtos');

    const titulo = `Produto CSV E2E ${Date.now()}`;
    const csv = `titulo;descricao;categoria;preco;precoOriginal;avaliacaoMedia;destaque;arquivoUrl;imagemUrl;vendaEmAtacado\n`
        + `${titulo};Produto criado via import CSV E2E.;PLANILHA;49,90;;;false;https://cdn.sawhub.com.br/e2e-produto.pdf;;false\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'produtos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linha = main.locator('[data-testid^="produto-row-"]', { hasText: titulo });
    await expect(linha).toBeVisible();
    await expect(linha.getByText('Rascunho')).toBeVisible();
  });

  test('importar CSV com categoria inválida não cria nada e mostra o erro por linha', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/produtos');

    const tituloValido = `Nao deveria entrar ${Date.now()}`;
    const csv = `titulo;descricao;categoria;preco;precoOriginal;avaliacaoMedia;destaque;arquivoUrl;imagemUrl;vendaEmAtacado\n`
        + `${tituloValido};Descrição.;PLANILHA;29,90;;;false;https://cdn.sawhub.com.br/valido.pdf;;false\n`
        + `Linha ruim;Descrição.;CATEGORIA_FALSA;29,90;;;false;https://cdn.sawhub.com.br/ruim.pdf;;false\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'produtos.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-erros')).toContainText('Linha 3');
    // Tudo-ou-nada: nem a linha válida do mesmo arquivo entrou.
    await expect(main.getByText(tituloValido)).not.toBeVisible();
  });
});
