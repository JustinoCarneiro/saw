import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M12 — E5 Mentorias & Atas (lado mentorado)', () => {
  test('mentorado vê a ata publicada e os materiais recomendados da mentoria realizada', async ({ page }) => {
    // João: mentoria seedada REALIZADA com ata PUBLICADA e "Ficha técnica — modelo" recomendada
    // (ver DemoDataSeeder.seedMentoriasEAtas, M12).
    await loginAs(page, 'joao@saborearte.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Mentorias & Atas' }).click();
    await expect(page).toHaveURL(/\/mentorado\/mentorias/);

    const historico = page.locator('[data-testid^="mentoria-historico-"]', { hasText: 'Lucas Alves' });
    await expect(historico).toBeVisible();
    await expect(historico.getByText('Realizada')).toBeVisible();

    await historico.getByTestId(/^expandir-ata-/).click();
    await expect(historico.getByText(/revisou os resultados do último mês/)).toBeVisible();
    await expect(historico.getByText('Ficha técnica — modelo')).toBeVisible();
  });

  test('ata ainda não publicada nunca aparece pro mentorado, mesmo com a mentoria já realizada', async ({ page }) => {
    // Ana/Carlos: mentoria em grupo REALIZADA cuja ata fica deliberadamente em RASCUNHO no seed
    // (demonstra a tela de revisão humana do Admin) — o mentorado não pode ver rascunho.
    await loginAs(page, 'ana@anacosta.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Mentorias & Atas' }).click();

    const historico = page.locator('[data-testid^="mentoria-historico-"]', { hasText: 'Ricardo Costa' });
    await expect(historico).toBeVisible();
    await historico.getByTestId(/^expandir-ata-/).click();
    await expect(historico.getByText('A ata desta mentoria ainda não foi publicada.')).toBeVisible();

    // A resposta crua da API também não pode carregar o resumo da IA de forma alguma.
    const res = await page.request.get('/api/v1/mentorado/mentorias');
    const mentorias: { ata: unknown }[] = await res.json();
    expect(mentorias.every((m) => m.ata === null)).toBe(true);
  });

  test('botão de entrar na reunião fica desabilitado fora da janela de horário, e o .ics é gerado', async ({ page }) => {
    // Rafael: próxima mentoria CONFIRMADA, vários dias no futuro — fora da janela de 10min.
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Mentorias & Atas' }).click();

    const agenda = page.locator('[data-testid^="mentoria-agenda-"]', { hasText: 'Lucas Alves' });
    await expect(agenda).toBeVisible();
    await expect(agenda.getByTestId(/^entrar-reuniao-/)).toBeDisabled();

    const googleLink = agenda.getByRole('link', { name: 'Google Calendar' });
    await expect(googleLink).toHaveAttribute('href', /calendar\.google\.com\/calendar\/render/);

    const mentoriaId = await agenda.getAttribute('data-testid').then((v) => v?.replace('mentoria-agenda-', ''));
    const res = await page.request.get(`/api/v1/mentorado/mentorias/${mentoriaId}/calendario.ics`);
    expect(res.status()).toBe(200);
    expect(res.headers()['content-type']).toContain('text/calendar');
    expect(await res.text()).toContain('BEGIN:VCALENDAR');
  });

  test('mentoria presencial (sem link online) não mostra botão de entrar na reunião', async ({ page }) => {
    // Fernanda/Marina: mentoria em grupo AGENDADA, presencial (local preenchido, linkOnline nulo).
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Mentorias & Atas' }).click();

    const agenda = page.locator('[data-testid^="mentoria-agenda-"]', { hasText: 'SAW HUB' });
    await expect(agenda).toBeVisible();
    await expect(agenda.getByTestId(/^entrar-reuniao-/)).toHaveCount(0);
  });
});
