import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M22 — import/export CSV de Mentorados (bulk-update apenas, resolvido por e-mail) e Comercial/
// Leads (import sempre cria em SOLICITACAO). Import é tudo-ou-nada (ver Blueprint, ROADMAP.md).
test.describe('Mentorados/Comercial — Import/Export CSV (M22)', () => {
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

  test('Mentorados: importar CSV atualiza um mentorado existente (resolvido por e-mail)', async ({ page }) => {
    const timestamp = Date.now();
    const nome = `Lead M22 E2E ${timestamp}`;
    const email = `m22.${timestamp}@example.com`;

    // Cria um mentorado de verdade, único desta execução — nunca muta um mentorado seedado
    // compartilhado com outras specs (vários deles têm nome/vencimentoPlano checados em
    // perfil.spec.ts/consolidated.spec.ts; mutar um deles via import quebraria essas asserções).
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');
    const mainComercial = page.getByRole('main');
    const linhaLead = mainComercial.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linhaLead.getByRole('button', { name: 'Mover p/ Em contato' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await linhaLead.getByRole('button', { name: 'Avançar p/ Proposta' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await linhaLead.getByRole('button', { name: 'Fechar venda' }).click();
    await page.getByLabel('Plano fechado').selectOption({ label: 'Básico' });
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linhaLead.getByText('Fechado', { exact: true })).toBeVisible();

    await page.context().clearCookies();
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Criar a partir de um lead' }).click();
    await page.getByLabel('Lead').selectOption({ label: `${nome} — ${email}` });
    await page.getByRole('button', { name: 'Criar mentorado' }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    // "Negócio Atualizado" (fixo) sozinho colide entre execuções — só o nome é único por
    // execução, então a checagem do negócio precisa ficar escopada à linha certa (mesma classe
    // de lição M09/M20: nunca assumir texto fixo como identificador único num teste repetível).
    const negocio = `Negócio Atualizado ${timestamp}`;
    const csv = `email;nome;negocio;plano;vencimentoPlano;status\n`
        + `${email};${nome} Atualizado;${negocio};PROFISSIONAL;;ATIVO\n`;

    // Mesmo raciocínio do teste de export acima: escopa ao widget de Mentorados (não Metas/Tarefas).
    const widgetMentorados = main.locator('xpath=//button[contains(text(),"Importar Mentorados")]/ancestor::div[contains(@class,"wrapper")]');
    await widgetMentorados.getByTestId('csv-importar-input').setInputFiles({
      name: 'mentorados.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(widgetMentorados.getByTestId('csv-import-sucesso')).toContainText('1 linha(s) importada(s)');
    const linhaAtualizada = main.locator('text=' + `${nome} Atualizado`).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linhaAtualizada.getByText(negocio)).toBeVisible();
  });

  test('Mentorados: importar CSV com e-mail inexistente mostra o erro por linha e não muta nada', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const csv = `email;nome;negocio;plano;vencimentoPlano;status\n`
        + `fantasma-m22-e2e@example.com;Qualquer;;GRATUITO;;ATIVO\n`;

    const main = page.getByRole('main');
    const widgetMentorados = main.locator('xpath=//button[contains(text(),"Importar Mentorados")]/ancestor::div[contains(@class,"wrapper")]');
    await widgetMentorados.getByTestId('csv-importar-input').setInputFiles({
      name: 'mentorados.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf-8'),
    });

    await expect(widgetMentorados.getByTestId('csv-import-erros')).toContainText('não encontrado');
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
