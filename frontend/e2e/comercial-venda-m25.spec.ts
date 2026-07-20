import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M25 (change request pós-MVP, 17/07/2026) — "formulário único de venda": produto/origem/valor/
// forma de pagamento substituindo o "planoFechado" solto, com etapa Diagnóstico opcional no funil
// e distribuição automática de parcelamento (Financeiro) e venda de ingresso (credenciamento).
test.describe('Comercial — formulário único de venda (M25)', () => {
  async function criarLeadAteProposta(page: import('@playwright/test').Page, nome: string): Promise<import('@playwright/test').Locator> {
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(`${nome.toLowerCase().replace(/\s+/g, '.')}@example.com`);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');
    const main = page.getByRole('main');
    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');

    await linha.getByRole('button', { name: 'Mover p/ Em contato' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Em contato', { exact: true })).toBeVisible();

    await linha.getByRole('button', { name: 'Avançar p/ Proposta' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Proposta', { exact: true })).toBeVisible();

    return linha;
  }

  test('Etapa Diagnóstico é opcional entre Em contato e Proposta', async ({ page }) => {
    const nome = `Diagnostico E2E ${Date.now()}`;
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(`diagnostico.e2e.${Date.now()}@example.com`);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');
    const main = page.getByRole('main');
    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');

    await linha.getByRole('button', { name: 'Mover p/ Em contato' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Em contato', { exact: true })).toBeVisible();

    await linha.getByRole('button', { name: 'Mover p/ Diagnóstico' }).click();
    await expect(page.getByText(`Mover "${nome}" para Diagnóstico`)).toBeVisible();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Diagnóstico', { exact: true })).toBeVisible();

    // A partir de Diagnóstico, o próximo passo continua sendo Proposta (mesma transição da
    // etapa Em contato — Lead.moverParaProposta() aceita os dois estados de origem).
    await linha.getByRole('button', { name: 'Avançar p/ Proposta' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Proposta', { exact: true })).toBeVisible();
  });

  test('Fechar venda com parcelamento cria contas a receber automaticamente no Financeiro', async ({ page }) => {
    const nome = `Parcelamento E2E ${Date.now()}`;
    const linha = await criarLeadAteProposta(page, nome);

    await linha.getByRole('button', { name: 'Fechar venda' }).click();
    await page.getByLabel('Produto vendido').selectOption({ label: 'Mentoria contínua' });
    await page.getByLabel('Origem da venda').selectOption({ label: 'Direta' });
    await page.getByLabel('Valor total da venda').fill('26000');
    await page.getByLabel('Valor pago no ato').fill('6000');
    await page.getByLabel('Forma de pagamento').selectOption({ label: 'Pix' });

    await page.getByRole('button', { name: '+ Adicionar parcela' }).click();
    await page.getByLabel(/Parcela 1 — valor/).fill('10000');
    await page.getByLabel('Data prevista').fill('2026-08-20');
    await page.getByRole('button', { name: '+ Adicionar parcela' }).click();
    await page.getByLabel(/Parcela 2 — valor/).fill('10000');
    await page.getByLabel('Data prevista').nth(1).fill('2026-09-20');

    await page.getByRole('button', { name: 'Confirmar venda' }).click();
    await expect(linha.getByText('Fechado', { exact: true })).toBeVisible();
    await expect(linha.getByText('Mentoria contínua · R$ 26.000,00')).toBeVisible();

    await page.context().clearCookies();
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.getByRole('link', { name: 'Financeiro' }).click();
    // Change request 20/07/2026 — "Contas a pagar/receber" fundida em "Lançamentos" (mesma tabela
    // desde o M26); a tela unificada mostra tudo por padrão (filtro de período desligado), então
    // as parcelas futuras aparecem sem precisar navegar pra um mês específico.
    await page.getByRole('link', { name: 'Lançamentos' }).click();
    const financeiroMain = page.getByRole('main');
    await expect(financeiroMain.getByText(`Parcela 1 - ${nome}`)).toBeVisible();
    await expect(financeiroMain.getByText(`Parcela 2 - ${nome}`)).toBeVisible();
  });

  test('Fechar venda de ingresso de evento credencia participantes nominalmente', async ({ page }) => {
    const nome = `Ingresso E2E ${Date.now()}`;
    const linha = await criarLeadAteProposta(page, nome);

    await linha.getByRole('button', { name: 'Fechar venda' }).click();
    await page.getByLabel('Produto vendido').selectOption({ label: 'Ingresso de evento' });
    await page.getByLabel('Origem da venda').selectOption({ label: 'Direta' });
    await page.getByLabel('Valor total da venda').fill('600');
    await page.getByLabel('Valor pago no ato').fill('600');
    await page.getByLabel('Forma de pagamento').selectOption({ label: 'Pix' });

    // Seletor de evento reaproveita GET /admin/comercial/eventos (M25) — EventoController
    // (Modulo.CONTEUDOS) não seria acessível pra uma vendedora só com Modulo.COMERCIAL. O rótulo
    // da opção inclui "(N vagas)" e N muda entre execuções (o seed só roda se a tabela estiver
    // vazia — não é um reset por subida — então outros testes já podem ter vendido ingresso pro
    // mesmo evento antes); resolve a opção pelo texto do evento, não pelo número de vagas.
    const eventoSelect = page.getByLabel('Evento do ingresso');
    const opcaoEvento = await eventoSelect.locator('option', { hasText: 'Encontro Nacional SAW 2026' }).textContent();
    await eventoSelect.selectOption({ label: opcaoEvento!.trim() });

    await page.getByRole('button', { name: '+ Adicionar ingresso' }).click();
    await page.getByLabel('Categoria').selectOption({ label: 'VIP' });
    await page.getByLabel('Nome do credenciado').fill('João Comprador');
    await page.getByLabel('Setor').fill('Financeiro');

    await page.getByRole('button', { name: 'Confirmar venda' }).click();
    await expect(linha.getByText('Fechado', { exact: true })).toBeVisible();
    await expect(linha.getByText('Ingresso de evento · R$ 600,00')).toBeVisible();
  });
});
