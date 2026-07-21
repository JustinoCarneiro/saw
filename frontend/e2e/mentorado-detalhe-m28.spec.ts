import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M28 (change request, 21/07/2026, "página dedicada de mentorado") — rota própria fora da
// MentoradosShell (/admin/mentorados/lista/:id), pensada como uma página do Notion do mentorado:
// métricas do Painel Consolidado no topo + histórico de mentorias (individual/grupo), acessível
// direto por URL (GET /admin/mentorados/{id}, endpoint novo — antes só existia listagem).
test.describe('M28 — Página dedicada de mentorado', () => {
  test('Ver perfil abre a página com métricas, seções e histórico de mentorias', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const main = page.getByRole('main');
    const linha = main.locator('text=Ana Costa').locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    // Header: nome como título da página (não mais uma linha de tabela), negócio/e-mail, status.
    await expect(page.getByRole('heading', { name: 'Ana Costa' })).toBeVisible();
    await expect(page.getByText('Cantina Ana Costa · ana@anacosta.com.br')).toBeVisible();

    // Métricas do Painel Consolidado reaproveitadas no topo da página.
    await expect(page.getByText('Progresso')).toBeVisible();
    await expect(page.getByText('Encaminhamentos')).toBeVisible();
    await expect(page.getByText('Crescimento de faturamento')).toBeVisible();

    // Todas as seções migradas da antiga tela inline continuam presentes, agora como página.
    // "Ferramentas obrigatórias" aparece 2x (label da métrica + título da seção) — .first() basta
    // pra provar presença, o teste não precisa distinguir qual das duas.
    await expect(page.getByText('Perfil', { exact: true })).toBeVisible();
    await expect(page.getByText('Dados de contrato', { exact: true })).toBeVisible();
    await expect(page.getByText('Diagnóstico Inicial', { exact: true })).toBeVisible();
    await expect(page.getByText('Ferramentas obrigatórias', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Acompanhamento', { exact: true })).toBeVisible();

    // Histórico de mentorias (M28 item D) — Ana participa da mentoria em grupo REALIZADA do seed
    // (DemoDataSeeder, mentor Ricardo Costa) — "Ver ata" navega pra AtaDetalhePage já existente.
    await expect(page.getByText('Mentorias', { exact: true })).toBeVisible();
    const linhaMentoria = page.locator('text=Ricardo Costa').locator('xpath=ancestor::div[contains(@class,"mentoriaRow")]');
    await expect(linhaMentoria.getByText('Realizada', { exact: true })).toBeVisible();
    await linhaMentoria.getByRole('button', { name: 'Ver ata' }).click();
    await expect(page).toHaveURL(/\/mentorias\/.+\/ata$/);
  });

  test('Voltar pra lista de Mentorados e acesso direto por URL', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const main = page.getByRole('main');
    const linha = main.locator('text=Ana Costa').locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);
    const url = page.url();

    await page.getByRole('button', { name: 'Mentorados', exact: true }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista$/);

    // Acesso direto por URL (bookmark/reload) — GET /admin/mentorados/{id}, endpoint novo do M28
    // (antes só existia a listagem, a tela sempre reaproveitava o objeto já carregado por ela).
    await page.goto(url);
    await expect(page.getByRole('heading', { name: 'Ana Costa' })).toBeVisible();
  });

  // Ativar/desativar já existia na listagem — M28 moveu o botão pra dentro da página dedicada,
  // precisa continuar funcionando de lá.
  test('Ativar/desativar a partir da página dedicada persiste o status', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const main = page.getByRole('main');
    const linha = main.locator('text=Rafael Gomes').locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    await page.getByRole('button', { name: 'Desativar' }).click();
    await page.getByRole('button', { name: 'Confirmar desativação' }).click();
    await expect(page.getByRole('button', { name: 'Ativar' })).toBeVisible();

    await page.reload();
    await expect(page.getByRole('button', { name: 'Ativar' })).toBeVisible();

    // Devolve o fixture ao estado original pra não quebrar outras specs que dependem de Rafael
    // Gomes ativo (mesma cautela já usada em specs que mutam fixtures compartilhados do seed).
    await page.getByRole('button', { name: 'Ativar' }).click();
    await expect(page.getByRole('button', { name: 'Desativar' })).toBeVisible();
  });
});
