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

    // M23 — gráfico de linha (réplica do mockup design/mockups-ref/06-admin.png, Tela 11):
    // path da linha sempre presente, mesmo com histórico zerado.
    await expect(page.getByText('Crescimento de mentorados')).toBeVisible();
    await expect(page.getByTestId('grafico-crescimento-mentorados').locator('svg path').first()).toBeVisible();

    // Distribuição por plano: donut (M23) + legenda com os 4 planos, mesmo que algum tenha 0.
    const distribuicao = page.getByTestId('grafico-distribuicao-plano');
    await expect(distribuicao.locator('svg circle, svg path').first()).toBeVisible();
    await expect(distribuicao.getByText('Gratuito')).toBeVisible();
    await expect(distribuicao.getByText('Básico')).toBeVisible();
    await expect(distribuicao.getByText('Essencial')).toBeVisible();
    await expect(distribuicao.getByText('Profissional')).toBeVisible();

    // Atividades recentes: a suíte completa já gera mentorados/eventos/conteúdos em outras
    // specs, então a lista real tem itens — confirma que não é o estado vazio.
    const atividades = page.getByTestId('atividades-recentes');
    await expect(atividades.getByText('Nenhuma atividade recente.')).toHaveCount(0);
    // M23 — ícone linear (SVG), não mais emoji (design/DESIGN.md §8).
    await expect(atividades.locator('svg').first()).toBeVisible();

    await expect(page.getByTestId('mentorias-hoje')).toBeVisible();
  });

  test('H10 — cancelar uma mentoria aparece nas atividades recentes (marco de transição, não só criação)', async ({ page }) => {
    // Achado desta leva: "atividades recentes" só cobria eventos de CRIAÇÃO (nenhuma entidade
    // rastreava a data de uma transição de status) — cancelar/realizar/pagar/reembolsar/
    // fechar/perder agora passam pelo AtividadeLog. Cancelar mentoria é o mais fácil de disparar
    // via UI num teste (não depende de gateway de pagamento nem funil de lead multi-etapa).
    //
    // Cria uma mentoria PRÓPRIA em vez de reusar/cancelar uma do seed: a única mentoria Agendada
    // seedada (Fernanda/Marina) é usada por mentorias.spec.ts pra testar a agenda do mentorado —
    // cancelá-la quebraria aquele teste (mentoria cancelada some da agenda).
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/mentorias');

    await page.getByRole('button', { name: 'Nova mentoria' }).click();
    await page.getByLabel('Mentor').selectOption({ label: 'Lucas Alves' });
    await page.getByPlaceholder('Buscar mentorado...').fill('Ana Costa');
    await page.getByLabel('Ana Costa').check();
    await page.getByLabel('Data e hora').fill('2026-09-01T10:00');
    await page.getByRole('button', { name: 'Criar mentoria' }).click();

    // "Ana Costa" sozinho não é único — ela também aparece como integrante da mentoria em grupo
    // seedada (Ricardo Costa, Realizada). Filtra também por "Agendada" (status da recém-criada,
    // Realizada não tem botão Cancelar) pra isolar a linha certa.
    const linha = page.locator('[data-testid^="mentoria-row-"]', { hasText: 'Ana Costa' }).filter({ hasText: 'Agendada' });
    await expect(linha).toBeVisible();
    await linha.getByRole('button', { name: 'Cancelar' }).click();
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/mentorias/') && res.url().includes('/status') && res.ok()),
      page.getByRole('alertdialog').getByRole('button', { name: 'Cancelar mentoria' }).click(),
    ]);

    await page.goto('/admin/dashboard');
    const atividades = page.getByTestId('atividades-recentes');
    await expect(atividades.getByText(/Mentoria cancelada:.*Ana Costa/).first()).toBeVisible();
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
