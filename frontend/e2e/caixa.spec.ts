import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// "Caixa do mês: Inicial, saldo por banco, Final" + "Transferências Entre Contas" (change request
// pós-MVP, E14, reunião 17/07/2026).
test.describe('Caixa do mês (E14)', () => {
  test('Fundador cadastra conta bancária, registra posição do mês e vê nos totais', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Caixa' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/caixa$/);

    const main = page.getByRole('main');
    const nomeConta = `Conta E2E ${Date.now()}`;

    await main.getByRole('button', { name: 'Nova conta bancária' }).click();
    await main.getByLabel('Nome da conta (ex.: Itaú, Infinity Pay)').fill(nomeConta);
    await main.getByRole('button', { name: 'Salvar conta' }).click();
    await expect(main.getByRole('button', { name: 'Salvar conta' })).toHaveCount(0);

    await main.getByRole('button', { name: 'Registrar posição do mês' }).click();
    await main.getByLabel('Conta bancária').selectOption({ label: nomeConta });
    await main.getByLabel('Saldo inicial (R$)').fill('1000');
    await main.getByLabel('Saldo final (R$)').fill('1500');
    await main.getByRole('button', { name: 'Registrar posição', exact: true }).click();

    await expect(main.getByText('Saldo por banco')).toBeVisible();
    // Escopado por testId (não .locator('..') do nome) — a suíte não reseta o banco entre
    // execuções, e mais de uma conta bancária no ar faz o nome sozinho não bastar mais pra achar
    // a linha certa (mesma classe de flake já documentada no LeadRepositoryTest do M05).
    const linhaConta = main.getByTestId('conta-caixa-row').filter({ hasText: nomeConta });
    await expect(linhaConta.getByText(/Inicial/)).toBeVisible();
    await expect(linhaConta.getByText(/Final/)).toBeVisible();
  });

  test('Fundador registra transferência entre contas e vê na listagem', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await page.getByRole('link', { name: 'Financeiro' }).click();
    await page.getByRole('link', { name: 'Caixa' }).click();
    await expect(page).toHaveURL(/\/admin\/financeiro\/caixa$/);

    const main = page.getByRole('main');
    const contaOrigem = `Origem E2E ${Date.now()}`;
    const contaDestino = `Destino E2E ${Date.now()}`;

    for (const nome of [contaOrigem, contaDestino]) {
      await main.getByRole('button', { name: 'Nova conta bancária' }).click();
      await main.getByLabel('Nome da conta (ex.: Itaú, Infinity Pay)').fill(nome);
      await main.getByRole('button', { name: 'Salvar conta' }).click();
      await expect(main.getByRole('button', { name: 'Salvar conta' })).toHaveCount(0);
    }

    const descricao = `Empréstimo interno E2E ${Date.now()}`;
    await main.getByRole('button', { name: 'Nova transferência' }).click();
    await main.getByLabel('Conta de origem').selectOption({ label: contaOrigem });
    await main.getByLabel('Conta de destino').selectOption({ label: contaDestino });
    await main.getByLabel('Valor (R$)').fill('250.50');
    await main.getByLabel('Descrição (opcional)').fill(descricao);
    await main.getByRole('button', { name: 'Registrar transferência' }).click();

    const linha = main.getByTestId('transferencia-row').filter({ hasText: descricao });
    await expect(linha).toBeVisible();
    await expect(linha.getByText(contaOrigem)).toBeVisible();
    await expect(linha.getByText(contaDestino)).toBeVisible();
  });
});
