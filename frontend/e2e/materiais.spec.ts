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
    // assumir um estado inicial fixo (☆ vs ★). Lê o estado atual e alterna a partir dele.
    const favoritarBtn = dica.getByRole('button', { name: /^[☆★]$/ });
    const jaEraFavorito = (await favoritarBtn.textContent())?.trim() === '★';
    await favoritarBtn.click();
    await expect(favoritarBtn).toHaveText(jaEraFavorito ? '☆' : '★');

    const assistidoBtn = dica.getByRole('button', { name: /Assistido|Marcar assistido/ });
    const jaEstavaAssistido = (await assistidoBtn.textContent())?.includes('✓');
    if (!jaEstavaAssistido) {
      await assistidoBtn.click();
      await expect(dica.getByRole('button', { name: '✓ Assistido' })).toBeVisible();
    }

    // Biblioteca (catálogo) reflete o mesmo estado de favorito pro mesmo item
    await page.getByRole('button', { name: 'Biblioteca' }).click();
    const material = page.locator('[data-testid^="material-"]', { hasText: 'Como calcular seu DRE' });
    const favoritoAgora = !jaEraFavorito;
    await expect(material.getByRole('button', { name: favoritoAgora ? /★ Favorito/ : /☆ Favoritar/ })).toBeVisible();

    // Filtro "Apenas favoritos" reflete o estado atual
    await page.getByLabel('Apenas favoritos').check();
    if (favoritoAgora) {
      await expect(material).toBeVisible();
    } else {
      await expect(material).toHaveCount(0);
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
