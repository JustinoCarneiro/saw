import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M08 — E2 Dashboard do Mentorado', () => {
  test('login como mentorado leva direto pro Dashboard (não mais pro placeholder de "em construção")', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await expect(page.getByText('Olá, Rafael')).toBeVisible();
  });

  test('Dashboard mostra dado real seedado: evolução, tarefas abertas, próxima reunião e dica', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    // Rafael Gomes: peso concluído 2 / peso total 11 = 18%; 6 encaminhamentos abertos
    // (ver DemoDataSeeder.seedMentorados) — mesmo cálculo do ProgressoCalculatorTest.
    await expect(page.getByText('18%')).toBeVisible();
    await expect(page.getByText('6', { exact: true })).toBeVisible();

    // Meta semanal ainda não existe (E3) — estado explícito, não um número inventado.
    await expect(page.getByText('Ainda não disponível')).toBeVisible();

    // m3 do seed: Mentoria individual confirmada, futura (15/07/2026) — aparece 2x (Próxima
    // reunião + Compromissos, já que é a única mentoria futura de Rafael).
    await expect(page.getByText('Mentoria individual').first()).toBeVisible();
    await expect(page.getByRole('link', { name: 'Entrar na reunião' })).toHaveAttribute('href', 'https://meet.google.com/rafael-lucas');

    await expect(page.getByText('Como calcular seu DRE')).toBeVisible();
  });

  test('Mentorado sem compromisso futuro vê o estado vazio, não erro', async ({ page }) => {
    // João Silva só tem uma mentoria REALIZADA (passada) no seed — sem próxima reunião.
    await loginAs(page, 'joao@saborearte.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await expect(page.getByText('Nenhuma reunião agendada.')).toBeVisible();
    await expect(page.getByText('Nenhum compromisso futuro.')).toBeVisible();
  });

  test('Avisos sempre mostram estado vazio (E16 não construído nesta leva)', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await expect(page.getByText('Nenhum aviso no momento.')).toBeVisible();
  });

  test('isolamento por tenant: Admin não acessa /api/v1/mentorado/dashboard', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    // page.request (não o fixture request avulso) reusa os cookies da sessão logada do Admin —
    // um request fixture isolado não teria sessão nenhuma e o 401 resultante não provaria nada
    // sobre RBAC, só sobre falta de login (mesma pegadinha já documentada no M07/google-oauth).
    const res = await page.request.get('/api/v1/mentorado/dashboard');
    expect(res.status()).toBe(403);
  });
});
