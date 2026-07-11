import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('RBAC por área', () => {
  test('Comercial só vê o módulo Comercial na sidebar e não acessa Time', async ({ page }) => {
    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);

    await expect(page.getByRole('link', { name: 'Comercial' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Time' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Painel Consolidado' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Financeiro' })).toHaveCount(0);

    // Navegação direta pela URL não deve furar o RBAC do frontend (o backend já barra com 403,
    // mas a tela também precisa recusar mostrar o módulo).
    await page.goto('/admin/time');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    // Financeiro (E14) é o único módulo restrito só ao Fundador — o backend reforça isso com
    // default-deny explícito no filter chain (M4), não só a checagem fina de @RequiresModulo.
    await page.goto('/admin/financeiro/dre');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.screenshot({ path: 'e2e/screenshots/rbac-comercial.png' });
  });

  test('Gestão de Performance vê Painel Consolidado mas não Time', async ({ page }) => {
    await loginAs(page, 'lucas@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);

    // getByRole('navigation') de propósito no link "Mentorados": a MentoradosShell (M06) tem
    // sua própria aba "Mentorados", texto igual ao link da sidebar — sem escopo, os dois batem.
    const sidebar = page.getByRole('navigation');
    await expect(page.getByRole('link', { name: 'Painel Consolidado' })).toBeVisible();
    await expect(sidebar.getByRole('link', { name: 'Mentorados' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Time' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Financeiro' })).toHaveCount(0);

    await page.getByRole('link', { name: 'Painel Consolidado' }).click();
    await expect(page.getByRole('main').getByText('João Silva').first()).toBeVisible();

    await page.goto('/admin/time');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.goto('/admin/financeiro/dre');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.screenshot({ path: 'e2e/screenshots/rbac-gestao-performance.png' });
  });

  // H15.3 (M20) — achado da auditoria de cobertura: RBAC de Marketing já existia no backend
  // (AreaModuloMatrix) desde o início do E15, mas nunca tinha E2E cobrindo o próprio papel.
  test('Marketing só vê o módulo Conteúdos na sidebar e não acessa Time/Comercial/Financeiro', async ({ page }) => {
    await loginAs(page, 'juliana@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);

    // getByRole('navigation') de propósito, mesmo raciocínio do teste de Gestão de Performance
    // acima: a ConteudosShell tem sua própria aba "Conteúdos", texto igual ao link da sidebar.
    const sidebar = page.getByRole('navigation');
    await expect(sidebar.getByRole('link', { name: 'Conteúdos' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Comercial' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Time' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Financeiro' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Painel Consolidado' })).toHaveCount(0);

    await page.goto('/admin/time');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.goto('/admin/comercial');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.goto('/admin/financeiro/dre');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.screenshot({ path: 'e2e/screenshots/rbac-marketing.png' });
  });
});
