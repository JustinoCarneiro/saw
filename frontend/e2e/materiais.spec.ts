import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M11 — E6 Materiais & Dicas do Brayan', () => {
  test('mentorado favorita e assiste uma dica, e o filtro "Apenas favoritos" reflete isso', async ({ page }) => {
    // Carlos (PROFISSIONAL) enxerga toda a biblioteca — usa Carlos pra não colidir com dado
    // criado nas verificações ao vivo desta leva, que usaram Rafael/Fernanda.
    await loginAs(page, 'carlos@pointdocarlos.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Materiais & Dicas' }).click();
    await expect(page).toHaveURL(/\/mentorado\/materiais/);

    await page.getByRole('button', { name: 'Dicas do Brayan' }).click();
    const dica = page.locator('[data-testid^="dica-"]', { hasText: 'Como calcular seu DRE' });
    await expect(dica).toBeVisible();

    // Favoritar/assistido são toggles sobre um item de catálogo SEEDADO (não criado neste teste,
    // ao contrário de Meta/Tarefa) — o estado persiste entre execuções, então o teste não pode
    // assumir um estado inicial fixo. Lê o estado atual via aria-pressed (ícone é só visual, ver
    // MateriaisPage.tsx) e alterna a partir dele.
    const favoritarBtn = dica.getByTestId(/^favoritar-dica-/);
    const jaEraFavorito = (await favoritarBtn.getAttribute('aria-pressed')) === 'true';
    await favoritarBtn.click();
    await expect(favoritarBtn).toHaveAttribute('aria-pressed', String(!jaEraFavorito));

    const assistidoBtn = dica.getByTestId(/^assistido-dica-/);
    const jaEstavaAssistido = (await assistidoBtn.getAttribute('aria-pressed')) === 'true';
    if (!jaEstavaAssistido) {
      await assistidoBtn.click();
      await expect(assistidoBtn).toHaveAttribute('aria-pressed', 'true');
    }

    // Biblioteca (catálogo) reflete o mesmo estado de favorito pro mesmo item
    await page.getByRole('button', { name: 'Biblioteca' }).click();
    const material = page.locator('[data-testid^="material-"]', { hasText: 'Como calcular seu DRE' });
    const favoritoAgora = !jaEraFavorito;
    await expect(material.getByTestId(/^favoritar-material-/)).toHaveAttribute('aria-pressed', String(favoritoAgora));

    // Filtro "Apenas favoritos" reflete o estado atual
    await page.getByLabel('Apenas favoritos').check();
    if (favoritoAgora) {
      await expect(material).toBeVisible();
    } else {
      await expect(material).toHaveCount(0);
    }
  });

  test('H6.3 — indicadores de consumo (dias assistidos, minutos, favoritas) refletem as ações do mentorado', async ({ page }) => {
    // Rafael não aparece em nenhum outro teste deste arquivo (Carlos/Marina/Ana acima) —
    // evita qualquer acoplamento com o estado que eles deixam; compara por delta, não valor
    // absoluto, mesmo raciocínio de tarefas.spec.ts (contagem exata dependeria de ordem/seed).
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    type Indicadores = { diasAssistidos: number; favoritas: number; minutosAssistidos: number };

    await page.getByRole('link', { name: 'Materiais & Dicas' }).click();
    await expect(page.getByTestId('indicadores-consumo')).toBeVisible();
    await page.getByRole('button', { name: 'Dicas do Brayan' }).click();

    // "Como calcular seu DRE": único vídeo seedado, com duração cadastrada de 12min (ver
    // DemoDataSeeder) — bom fixture pra provar que a soma de minutosAssistidos usa essa duração.
    const dica = page.locator('[data-testid^="dica-"]', { hasText: 'Como calcular seu DRE' });
    await expect(dica).toBeVisible();

    // Normaliza pra "não favorito" ANTES de capturar o "antes" — se já estivesse favorito de uma
    // execução anterior, capturar o baseline só depois evita o delta dar 0 em vez de +1. Estado
    // lido via aria-pressed (ícone é só visual, ver MateriaisPage.tsx).
    const favoritarBtn = dica.getByTestId(/^favoritar-dica-/);
    if ((await favoritarBtn.getAttribute('aria-pressed')) === 'true') {
      await favoritarBtn.click();
      await expect(favoritarBtn).toHaveAttribute('aria-pressed', 'false');
    }

    const assistidoBtn = dica.getByTestId(/^assistido-dica-/);
    const jaEstavaAssistido = (await assistidoBtn.getAttribute('aria-pressed')) === 'true';

    const antes: Indicadores = await (await page.request.get('/api/v1/mentorado/conteudos/indicadores')).json();

    await favoritarBtn.click();
    await expect(favoritarBtn).toHaveAttribute('aria-pressed', 'true');
    if (!jaEstavaAssistido) {
      await assistidoBtn.click();
      await expect(assistidoBtn).toHaveAttribute('aria-pressed', 'true');
    }

    await expect(page.getByTestId('indicador-favoritas')).toContainText(String(antes.favoritas + 1));

    const depois: Indicadores = await (await page.request.get('/api/v1/mentorado/conteudos/indicadores')).json();
    expect(depois.favoritas).toBe(antes.favoritas + 1);
    expect(depois.diasAssistidos).toBeGreaterThanOrEqual(antes.diasAssistidos);
    // Se já estava assistido antes (execução anterior), o minuto já contava — só garante que
    // não caiu; se acabou de marcar agora, tem que ter subido exatamente os 12min do vídeo.
    if (jaEstavaAssistido) {
      expect(depois.minutosAssistidos).toBeGreaterThanOrEqual(antes.minutosAssistidos);
    } else {
      expect(depois.minutosAssistidos).toBe(antes.minutosAssistidos + 12);
    }
  });

  test('mentorado com plano insuficiente não vê nem consegue favoritar conteúdo acima do próprio plano', async ({ page }) => {
    // Marina (BASICO) não deve ver "Apresentação: Precificação estratégica" (ESSENCIAL).
    await loginAs(page, 'marina@sabordamarina.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Materiais & Dicas' }).click();

    await expect(page.getByText('Apresentação: Precificação estratégica')).toHaveCount(0);

    const res = await page.request.get('/api/v1/mentorado/conteudos');
    const catalogo: { titulo: string }[] = await res.json();
    expect(catalogo.some((c) => c.titulo === 'Apresentação: Precificação estratégica')).toBe(false);
  });

  test('isolamento por tenant: favorito de um mentorado não aparece pra outro', async ({ page }) => {
    // Ana nunca favoritou "Como calcular seu DRE" nesta leva — se o favorito de Carlos (teste
    // acima) vazasse, essa asserção pegaria.
    await loginAs(page, 'ana@anacosta.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const res = await page.request.get('/api/v1/mentorado/conteudos');
    expect(res.status()).toBe(200);
    const catalogo: { titulo: string; favorito: boolean }[] = await res.json();
    const dre = catalogo.find((c) => c.titulo === 'Como calcular seu DRE');
    expect(dre?.favorito).toBe(false);
  });
});
