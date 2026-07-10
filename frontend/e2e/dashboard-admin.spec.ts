import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M16 — E10 Painel Administrativo & Métricas', () => {
  test('Fundador vê o dashboard com KPIs, crescimento, distribuição, atividades e mentorias de hoje', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    // DASHBOARD é o primeiro módulo da ordem de redirecionamento pra quem tem acesso a ele
    // (só Fundador) — login já cai direto em /admin/dashboard, sem precisar navegar.
    await expect(page).toHaveURL(/\/admin\/dashboard/);

    // Dado real seedado/acumulado por outras suítes E2E — não hardcoda valor absoluto, só
    // confirma que os KPIs carregaram com número (não vazio/erro). .first(): o "hint" de
    // variação (ex.: "0.0% este mês") também casa com /\d+/, não é ambiguidade real.
    await expect(page.getByTestId('kpi-mentorados-ativos').getByText(/\d+/).first()).toBeVisible();
    await expect(page.getByTestId('kpi-mentorias-realizadas').getByText(/\d+/).first()).toBeVisible();
    await expect(page.getByTestId('kpi-eventos-realizados').getByText(/\d+/).first()).toBeVisible();
    await expect(page.getByTestId('kpi-receita-mes').getByText('R$', { exact: false })).toBeVisible();

    // Crescimento de mentorados: sempre 6 meses (Blueprint M16), independente do dado real.
    await expect(page.getByText('Crescimento de mentorados')).toBeVisible();

    // Distribuição por plano: sempre os 4 planos, mesmo que algum tenha 0 mentorados.
    await expect(page.getByText('Gratuito')).toBeVisible();
    await expect(page.getByText('Básico')).toBeVisible();
    await expect(page.getByText('Essencial')).toBeVisible();
    await expect(page.getByText('Profissional')).toBeVisible();

    // Atividades recentes: a suíte completa já gera mentorados/eventos/conteúdos em outras
    // specs, então a lista real tem itens — confirma que não é o estado vazio.
    const atividades = page.getByTestId('atividades-recentes');
    await expect(atividades.getByText('Nenhuma atividade recente.')).toHaveCount(0);

    await expect(page.getByTestId('mentorias-hoje')).toBeVisible();
  });

  test('isolamento por RBAC: área Comercial não vê "Dashboard" na sidebar nem acessa via URL direta', async ({ page }) => {
    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    // Comercial não tem Modulo.DASHBOARD — cai direto no primeiro módulo que TEM (Comercial).
    await expect(page).not.toHaveURL(/\/admin\/dashboard/);

    // getByRole('navigation') de propósito (mesmo padrão de rbac.spec.ts pro link
    // "Mentorados"): a ComercialShell tem sua própria aba interna "Dashboard"
    // (/admin/comercial/dashboard), texto igual ao link da sidebar — sem escopo, os dois batem.
    const sidebar = page.getByRole('navigation');
    await expect(sidebar.getByRole('link', { name: 'Dashboard' })).toHaveCount(0);

    await page.goto('/admin/dashboard');
    await expect(page.getByText('Sem acesso')).toBeVisible();
  });
});
