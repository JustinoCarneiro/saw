import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('RBAC por área', () => {
  test('Comercial só vê o módulo Comercial na sidebar e não acessa Time', async ({ page }) => {
    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);

    await expect(page.getByRole('link', { name: 'Comercial' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Time' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Gestão de Performance' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Financeiro' })).toHaveCount(0);

    // Navegação direta pela URL não deve furar o RBAC do frontend (o backend já barra com 403,
    // mas a tela também precisa recusar mostrar o módulo).
    await page.goto('/admin/time');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    // Painel Consolidado não tem rota própria — virou a 1ª aba dentro de Mentorados, então
    // barrar /admin/mentorados também cobre o Painel Consolidado (mesmo Modulo.MENTORADOS).
    await page.goto('/admin/mentorados');
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

    // getByRole('navigation') de propósito no link "Gestão de Performance": a MentoradosShell
    // (M06) tem sua própria aba "Gestão de Performance" (M28, renomeada de "Mentorados"), texto
    // igual ao link da sidebar E ao nome da própria área RBAC testada aqui — coincidência aceita
    // pelo cliente (ver ROADMAP.md § M28) — sem escopo, os três batem.
    const sidebar = page.getByRole('navigation');
    await expect(sidebar.getByRole('link', { name: 'Gestão de Performance' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Time' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Financeiro' })).toHaveCount(0);

    // Painel Consolidado não é mais item próprio da sidebar — virou a 1ª aba (index) dentro de
    // Mentorados/Gestão de Performance, então quem tem Modulo.MENTORADOS cai direto nela ao
    // clicar "Gestão de Performance".
    await sidebar.getByRole('link', { name: 'Gestão de Performance' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/consolidado$/);
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
    await expect(page.getByRole('link', { name: 'Gestão de Performance' })).toHaveCount(0);

    await page.goto('/admin/time');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.goto('/admin/comercial');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.goto('/admin/financeiro/dre');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    // Painel Consolidado não tem rota própria — virou a 1ª aba dentro de Mentorados, mesmo
    // Modulo.MENTORADOS que Marketing não tem.
    await page.goto('/admin/mentorados');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.screenshot({ path: 'e2e/screenshots/rbac-marketing.png' });
  });
});
