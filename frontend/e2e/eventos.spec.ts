import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M13 — E7 Eventos & Inscrições (lado mentorado)', () => {
  test('mentorado já inscrito (seed) vê o evento em "Próximos eventos"', async ({ page }) => {
    // João: seedado com inscrição ativa no "Encontro Nacional SAW 2026" (ver DemoDataSeeder, M13).
    await loginAs(page, 'joao@saborearte.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Eventos' }).click();
    await expect(page).toHaveURL(/\/mentorado\/eventos/);

    await expect(page.getByText('Próximos eventos (você está inscrito)')).toBeVisible();
    const meuEvento = page.locator('[data-testid^="evento-"]', { hasText: 'Encontro Nacional SAW 2026' });
    await expect(meuEvento).toBeVisible();
    await expect(meuEvento.getByRole('button', { name: /Inscrito — cancelar/ })).toBeVisible();
  });

  test('mentorado se inscreve, evento entra em "Próximos eventos", e cancela em seguida', async ({ page }) => {
    // Rafael: sem inscrições seedadas — fluxo completo de inscrever/cancelar do zero.
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Eventos' }).click();

    const workshop = page.locator('[data-testid^="evento-"]', { hasText: 'Workshop de Gestão Financeira' });
    await expect(workshop.getByRole('button', { name: 'Inscrever-se' })).toBeVisible();
    await workshop.getByRole('button', { name: 'Inscrever-se' }).click();

    await expect(page.getByText('Próximos eventos (você está inscrito)')).toBeVisible();
    const meuWorkshop = page.locator('[data-testid^="evento-"]', { hasText: 'Workshop de Gestão Financeira' });
    await expect(meuWorkshop.getByRole('button', { name: /Inscrito — cancelar/ })).toBeVisible();

    const res1 = await page.request.get('/api/v1/mentorado/eventos');
    const eventos1: { titulo: string; inscrito: boolean }[] = await res1.json();
    expect(eventos1.find((e) => e.titulo === 'Workshop de Gestão Financeira')?.inscrito).toBe(true);

    // M23 — cancelar inscrição agora abre um ConfirmDialog antes de cancelar de verdade.
    await meuWorkshop.getByRole('button', { name: /Inscrito — cancelar/ }).click();
    await page.getByRole('button', { name: 'Cancelar inscrição' }).click();
    await expect(page.getByText('Próximos eventos (você está inscrito)')).toHaveCount(0);

    const res2 = await page.request.get('/api/v1/mentorado/eventos');
    const eventos2: { titulo: string; inscrito: boolean }[] = await res2.json();
    expect(eventos2.find((e) => e.titulo === 'Workshop de Gestão Financeira')?.inscrito).toBe(false);
  });

  test('isolamento por tenant: inscrição de um mentorado não aparece pra outro', async ({ page }) => {
    // Ana nunca se inscreveu em nada nesta leva — se a inscrição de João (Encontro Nacional)
    // vazasse, essa asserção pegaria.
    await loginAs(page, 'ana@anacosta.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const res = await page.request.get('/api/v1/mentorado/eventos');
    expect(res.status()).toBe(200);
    const eventos: { titulo: string; inscrito: boolean }[] = await res.json();
    const encontro = eventos.find((e) => e.titulo === 'Encontro Nacional SAW 2026');
    expect(encontro?.inscrito).toBe(false);
  });

  test('calendário: selecionar um dia com evento filtra a lista pra só aquele dia', async ({ page }) => {
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Eventos' }).click();

    const calendario = page.getByTestId('calendario-eventos');
    // "Encontro Nacional SAW 2026" é 10/09/2026 — calendário abre no mês corrente (julho/2026,
    // ver currentDate do ambiente), precisa avançar 2 meses (julho -> agosto -> setembro).
    await calendario.getByRole('button', { name: '›' }).click();
    await calendario.getByRole('button', { name: '›' }).click();
    await calendario.getByTestId('dia-2026-09-10').click();

    await expect(page.getByText('Eventos neste dia')).toBeVisible();
    await expect(page.locator('[data-testid^="evento-"]', { hasText: 'Encontro Nacional SAW 2026' })).toBeVisible();
    await expect(page.locator('[data-testid^="evento-"]', { hasText: 'Workshop de Gestão Financeira' })).toHaveCount(0);
  });
});
