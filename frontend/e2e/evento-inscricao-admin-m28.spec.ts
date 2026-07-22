import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// selectOption({ label }) exige string exata (sem regex) — o rótulo da opção inclui a data
// formatada ("Título — dd/mm/aaaa"), que este teste não monta caractere a caractere. Resolve o
// texto exato via hasText primeiro (mesmo padrão de comercial-venda-m25.spec.ts pro seletor de
// evento), só então seleciona por label.
async function selecionarEventoPorTitulo(page: import('@playwright/test').Page, titulo: string) {
  const select = page.getByLabel('Inscrever em');
  const opcao = await select.locator('option', { hasText: titulo }).textContent();
  await select.selectOption({ label: opcao!.trim() });
}

// M28 (change request, 21/07/2026, "controle de vagas em evento por mentorado da Contínua") —
// achado da investigação: a área do mentorado está pausada (AREA_MENTORADO_PAUSADA), então o
// self-service de inscrição em evento (EventoMentoradoController) é inalcançável na prática. Esta
// leva criou o único jeito hoje de inscrever alguém: pela página dedicada do mentorado (Fase C).
test.describe('M28 — Inscrição em evento pelo admin + cota de 3 grátis/ano (Mentoria Contínua)', () => {
  test('Admin inscreve e cancela um mentorado num evento', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const timestamp = Date.now();
    const nome = `Mentorado Evento E2E ${timestamp}`;
    const main = page.getByRole('main');

    await main.getByRole('button', { name: 'Criar mentorado direto' }).click();
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(`evento.${timestamp}@example.com`);
    await page.getByLabel('Tipo de contrato').selectOption({ label: 'Mentoria Individual' });
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    // Mentoria Individual não tem cota — não deve aparecer nenhum resumo de cota.
    await expect(page.getByText(/eventos grátis usados/)).toHaveCount(0);

    await selecionarEventoPorTitulo(page, 'Encontro Nacional SAW 2026');
    await page.getByRole('button', { name: 'Inscrever', exact: true }).click();
    await expect(page.getByText('Encontro Nacional SAW 2026')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancelar inscrição' })).toBeVisible();

    // Reinscrever no mesmo evento some da lista de opções (idempotência do lado do backend, mas
    // aqui é sobre a UI já filtrar quem já está inscrito ativamente).
    const opcoes = await page.getByLabel('Inscrever em').locator('option').allTextContents();
    expect(opcoes.some((o) => o.includes('Encontro Nacional SAW 2026'))).toBe(false);

    await page.getByRole('button', { name: 'Cancelar inscrição' }).click();
    await expect(page.getByRole('button', { name: 'Cancelar inscrição' })).toHaveCount(0);
    await expect(page.getByText('Cancelada', { exact: true })).toBeVisible();

    // Cancelada libera a vaga — evento volta a aparecer nas opções.
    const opcoesApos = await page.getByLabel('Inscrever em').locator('option').allTextContents();
    expect(opcoesApos.some((o) => o.includes('Encontro Nacional SAW 2026'))).toBe(true);
  });

  test('Cota de 3 eventos grátis/ano de contrato bloqueia o 4º evento pra Mentoria Contínua', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);

    // Cria 4 eventos PRÓPRIOS (não depende de quantos eventos programados já existem no seed/
    // outras specs) pra ter vaga garantida em todos.
    const timestamp = Date.now();
    const titulos = [0, 1, 2, 3].map((i) => `Evento Cota E2E ${timestamp}-${i}`);
    await page.goto('/admin/conteudos/eventos');
    const mainConteudos = page.getByRole('main');
    for (const titulo of titulos) {
      await mainConteudos.getByRole('button', { name: 'Novo evento' }).click();
      await page.getByLabel('Título').fill(titulo);
      await page.getByLabel('Data e hora').fill('2026-10-15');
      await page.getByLabel('Hora', { exact: true }).selectOption('19');
      await page.getByLabel('Minuto', { exact: true }).selectOption('00');
      await mainConteudos.getByRole('button', { name: 'Salvar' }).click();
      await expect(mainConteudos.getByText(titulo)).toBeVisible();
    }

    // Cria um mentorado Mentoria Contínua com data de fechamento recente (ciclo atual em
    // andamento) — cota de 3 grátis se aplica a partir daqui.
    await page.goto('/admin/mentorados/lista');
    const nome = `Mentorado Cota E2E ${timestamp}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Criar mentorado direto' }).click();
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(`cota.${timestamp}@example.com`);
    await page.getByLabel('Tipo de contrato').selectOption({ label: 'Mentoria Contínua' });
    await page.getByLabel('Data de fechamento').fill('2026-01-01');
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    await expect(page.getByText('0/3 eventos grátis usados neste ciclo de contrato')).toBeVisible();

    // Inscreve nos 3 primeiros — cada um deve funcionar e o contador deve subir.
    for (let i = 0; i < 3; i++) {
      await selecionarEventoPorTitulo(page, titulos[i]);
      await page.getByRole('button', { name: 'Inscrever', exact: true }).click();
      await expect(page.getByText(`${i + 1}/3 eventos grátis usados neste ciclo de contrato`)).toBeVisible();
    }

    // O 4º evento é bloqueado pela cota — mensagem de erro clara, nenhuma inscrição nova criada.
    await selecionarEventoPorTitulo(page, titulos[3]);
    await page.getByRole('button', { name: 'Inscrever', exact: true }).click();
    await expect(page.getByText(/Cota de 3 eventos grátis deste ciclo de contrato já foi usada/)).toBeVisible();
    // Continua 3/3 (não subiu pra 4) — a 4ª tentativa não criou inscrição nenhuma.
    await expect(page.getByText('3/3 eventos grátis usados neste ciclo de contrato')).toBeVisible();
  });
});
