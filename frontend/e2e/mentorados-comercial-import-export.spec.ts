import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M22 — export CSV de Mentorados e import/export de Comercial/Leads (import sempre cria em
// SOLICITACAO). M28 (21/07/2026) substituiu o import de Mentorados (era bulk-update só, 6 campos)
// pelo import único (cria OU atualiza, 19 campos) — ver testes abaixo. Import é tudo-ou-nada (ver
// Blueprint, ROADMAP.md).
test.describe('Mentorados/Comercial — Import/Export CSV (M22/M28)', () => {
  test('Mentorados: exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    // Bulk import (M23) trouxe CsvImportExport pra Metas e Tarefas na mesma tela — testId genérico
    // (data-testid="csv-exportar") deixou de ser único na página, precisa filtrar pelo label
    // (labelPrefix="Mentorados") pra não colidir com os widgets de Metas/Tarefas.
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Exportar Mentorados' }).click(),
    ]);

    expect(download.suggestedFilename()).toBe('mentorados.csv');
  });

  // M28 (change request, 21/07/2026, "import único") — o import estreito bulk-UPDATE (6 campos,
  // widget CsvImportExport) foi removido: dois botões de import confundiam o time. O único import
  // que resta em Mentorados é "Importar mentorados (CSV)" (MentoradoDiretoCsvService, 19 colunas,
  // Comercial), que cria OU atualiza dependendo de o e-mail já existir.
  test('Mentorados: import único (CSV) atualiza um mentorado existente resolvido por e-mail', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const timestamp = Date.now();
    const nome = `Mentorado M28 E2E ${timestamp}`;
    const email = `m28.${timestamp}@example.com`;
    const main = page.getByRole('main');

    // Cria um mentorado de verdade, único desta execução — nunca muta um mentorado seedado
    // compartilhado com outras specs (mesma cautela do M22: mutar um deles via import quebraria
    // asserções de perfil.spec.ts/consolidated.spec.ts).
    await main.getByRole('button', { name: 'Criar mentorado direto' }).click();
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Tipo de contrato').selectOption({ label: 'Mentoria Individual' });
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    // "Negócio Atualizado" (fixo) sozinho colide entre execuções — só o nome é único por
    // execução, então a checagem do negócio precisa ficar escopada à linha certa (mesma classe
    // de lição M09/M20: nunca assumir texto fixo como identificador único num teste repetível).
    const nomeAtualizado = `${nome} Atualizado`;
    const negocio = `Negócio Atualizado ${timestamp}`;
    const csv = 'email;nome;negocio;nomeFantasia;cnpj;socios;telefone;tipoContrato;valorContrato;'
        + 'dataFechamentoContrato;faturamentoAnual;quantidadeColaboradores;empresaRegularizada;quantidadeLojas;'
        + 'cmvDefinido;cmvDetalhe;tempoMedioAtendimento;culturaConstruida;processosDesenhados\n'
        + `${email};${nomeAtualizado};${negocio};;;;;MENTORIA_CONTINUA;;;;;;;;;;;\n`;

    await main.getByRole('button', { name: 'Importar mentorados (CSV)' }).click();
    await page.getByLabel('Arquivo CSV').setInputFiles({
      name: 'mentorados-direto.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });
    await page.getByRole('button', { name: 'Importar', exact: true }).click();

    await expect(page.getByText('0 criado(s), 1 atualizado(s)')).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    const linhaAtualizada = main.locator('text=' + nomeAtualizado).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linhaAtualizada.getByText(negocio)).toBeVisible();
  });

  test('Mentorados: import único (CSV) rejeita e-mail que já existe mas não é mentorado', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    // matheus@sawhub.com.br é login de Colaborador (Fundador), não de Mentorado — o import único
    // trata isso como erro de validação (e-mail em uso por outro tipo de conta), não como criação.
    const csv = 'email;nome;negocio;nomeFantasia;cnpj;socios;telefone;tipoContrato;valorContrato;'
        + 'dataFechamentoContrato;faturamentoAnual;quantidadeColaboradores;empresaRegularizada;quantidadeLojas;'
        + 'cmvDefinido;cmvDetalhe;tempoMedioAtendimento;culturaConstruida;processosDesenhados\n'
        + 'matheus@sawhub.com.br;Qualquer;;;;;;MENTORIA_CONTINUA;;;;;;;;;;;\n';

    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Importar mentorados (CSV)' }).click();
    await page.getByLabel('Arquivo CSV').setInputFiles({
      name: 'mentorados-direto.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });
    await page.getByRole('button', { name: 'Importar', exact: true }).click();

    await expect(page.getByText(/já existe.*não é um mentorado/)).toBeVisible();
  });

  test('Comercial: exportar CSV dispara o download do arquivo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByTestId('csv-exportar').click(),
    ]);

    expect(download.suggestedFilename()).toBe('leads.csv');
  });

  test('Comercial: importar CSV cria leads novos sempre em Solicitação', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');

    const nome = `Lead CSV Import E2E ${Date.now()}`;
    const csv = `nome;email;telefone;mensagem;planoInteresse\n`
        + `${nome};leadcsv.${Date.now()}@example.com;;Quero saber mais;ESSENCIAL\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'leads.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Solicitação')).toBeVisible();
  });

  test('Comercial: importar CSV com uma linha inválida não cria nenhum lead', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');

    const nomeValido = `Nao deveria entrar M22 ${Date.now()}`;
    const csv = `nome;email;telefone;mensagem;planoInteresse\n`
        + `${nomeValido};valido.${Date.now()}@example.com;;;\n`
        + `Sem email;;;;\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'leads.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-erros')).toContainText('Linha 3');
    await expect(main.getByText(nomeValido)).not.toBeVisible();
  });
});

// Fase 5 — achado ao vivo (curl direto): GET /admin/metas/export e /admin/encaminhamentos/export
// retornavam 500 (LazyInitializationException em `mentorado`, nunca coberto por E2E até aqui) e
// não existia NENHUMA tela pra ver o resultado do import — só os botões cegos de CSV, movidos da
// página de Mentorados pras abas dedicadas "Metas"/"Tarefas" nesta mesma leva.
test.describe('Metas/Tarefas — Import/Export CSV e listagem admin (Fase 5)', () => {
  test('Metas: exportar CSV dispara o download (regressão do 500 por LazyInitializationException)', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/metas');

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Exportar Metas' }).click(),
    ]);

    expect(download.suggestedFilename()).toBe('metas.csv');
  });

  test('Metas: importar CSV cria a meta e ela aparece na listagem admin', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/metas');

    const titulo = `Meta E2E Fase5 ${Date.now()}`;
    const csv = `emailMentorado;titulo;descricao;prazo\n`
        + `joao@saborearte.com.br;${titulo};Descrição de teste;31/12/2026\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'metas.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linha = main.locator('text=' + titulo).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('João Silva')).toBeVisible();
  });

  test('Tarefas: exportar CSV dispara o download (regressão do 500 por LazyInitializationException)', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/tarefas');

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Exportar Tarefas' }).click(),
    ]);

    expect(download.suggestedFilename()).toBe('tarefas.csv');
  });

  test('Tarefas: importar CSV cria a tarefa e ela aparece na listagem admin', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/tarefas');

    const titulo = `Tarefa E2E Fase5 ${Date.now()}`;
    const csv = `emailMentorado;titulo;prazo;prioridade\n`
        + `joao@saborearte.com.br;${titulo};31/12/2026;ALTA\n`;

    const main = page.getByRole('main');
    await main.getByTestId('csv-importar-input').setInputFiles({
      name: 'tarefas.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(main.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linha = main.locator('text=' + titulo).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('João Silva')).toBeVisible();
  });
});
