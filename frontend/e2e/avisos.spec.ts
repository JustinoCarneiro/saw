import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// Título único gerado uma vez, compartilhado entre os 3 testes de leitura/isolamento abaixo —
// cada teste roda com login/página isolados (loginAs não pode ser chamado 2x na mesma page:
// /login redireciona quem já está autenticado, então trocar de mentorado precisa de um teste
// novo, não de um 2º loginAs() na mesma execução).
const TITULO_LEITURA = `Aviso Leitura ${Date.now()}`;

test.describe('M17 — E16 Avisos & Notificações', () => {
  test('mentorado vê a lista de avisos seedados com categorias e o sino com contador', async ({ page }) => {
    await loginAs(page, 'joao@saborearte.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await expect(page.getByTestId('sino-avisos')).toBeVisible();

    await page.getByRole('link', { name: 'Avisos' }).click();
    await expect(page).toHaveURL(/\/mentorado\/avisos/);

    await expect(page.getByText('Manutenção programada')).toBeVisible();
    await expect(page.getByText('Workshop de Precificação Estratégica')).toBeVisible();
    await expect(page.getByText('Novo material na biblioteca')).toBeVisible();
    await expect(page.getByText('Nova mentoria em grupo disponível')).toBeVisible();

    await page.getByTestId('avisos-filtro-materiais').click();
    await expect(page.getByText('Novo material na biblioteca')).toBeVisible();
    await expect(page.getByText('Workshop de Precificação Estratégica')).toHaveCount(0);
  });

  test('Admin publica um aviso novo e ele aparece pro mentorado', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);

    await page.goto('/admin/conteudos/avisos');
    const titulo = `Aviso E2E ${Date.now()}`;
    await page.getByRole('button', { name: '+ Novo aviso' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Descrição').fill('Publicado pelo teste E2E.');
    await page.getByLabel('Categoria').selectOption('GERAL');
    await page.getByLabel(/Visível a partir do plano/).selectOption('GRATUITO');
    await page.getByRole('button', { name: 'Publicar' }).click();

    await expect(page.locator('[data-testid^="aviso-row-"]', { hasText: titulo })).toBeVisible();
  });

  test('Admin edita um aviso existente e depois exclui', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);

    await page.goto('/admin/conteudos/avisos');
    const titulo = `Aviso Editar E2E ${Date.now()}`;
    await page.getByRole('button', { name: '+ Novo aviso' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Descrição').fill('Original.');
    await page.getByLabel('Categoria').selectOption('GERAL');
    await page.getByLabel(/Visível a partir do plano/).selectOption('GRATUITO');
    await page.getByRole('button', { name: 'Publicar' }).click();

    const linha = page.locator('[data-testid^="aviso-row-"]', { hasText: titulo });
    await expect(linha).toBeVisible();

    const tituloEditado = `${titulo} (editado)`;
    await linha.getByRole('button', { name: 'Editar' }).click();
    await expect(page.getByText('Editar aviso')).toBeVisible();
    await page.getByLabel('Título').fill(tituloEditado);
    await page.getByRole('button', { name: 'Salvar' }).click();

    await expect(page.locator('[data-testid^="aviso-row-"]', { hasText: tituloEditado })).toBeVisible();
    await expect(page.getByText(titulo, { exact: true })).toHaveCount(0);

    const linhaEditada = page.locator('[data-testid^="aviso-row-"]', { hasText: tituloEditado });
    await linhaEditada.getByRole('button', { name: 'Excluir' }).click();
    const dialogo = page.getByRole('alertdialog');
    await expect(dialogo.getByText('Excluir aviso?')).toBeVisible();
    await dialogo.getByRole('button', { name: 'Excluir', exact: true }).click();

    await expect(page.locator('[data-testid^="aviso-row-"]', { hasText: tituloEditado })).toHaveCount(0);
  });

  test('setup: Admin publica o aviso usado nos testes de leitura/isolamento abaixo', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);

    await page.goto('/admin/conteudos/avisos');
    await page.getByRole('button', { name: '+ Novo aviso' }).click();
    await page.getByLabel('Título').fill(TITULO_LEITURA);
    await page.getByLabel('Descrição').fill('Testando marcar como lido.');
    await page.getByLabel('Categoria').selectOption('GERAL');
    await page.getByLabel(/Visível a partir do plano/).selectOption('GRATUITO');
    await page.getByRole('button', { name: 'Publicar' }).click();

    await expect(page.locator('[data-testid^="aviso-row-"]', { hasText: TITULO_LEITURA })).toBeVisible();
  });

  test('mentorado marca um aviso como lido clicando nele, e ele some da aba "Não lidos"', async ({ page }) => {
    await loginAs(page, 'carlos@pointdocarlos.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.goto('/mentorado/avisos');

    const cartao = page.locator('[data-testid^="aviso-"]', { hasText: TITULO_LEITURA });
    await expect(cartao).toBeVisible();
    await expect(cartao.locator('[data-testid^="aviso-nao-lido-"]')).toHaveCount(1);

    // click() só dispara o evento — não espera o PATCH assíncrono do onClick terminar.
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/avisos/') && res.url().includes('/lido') && res.ok()),
      cartao.click(),
    ]);
    await expect(cartao.locator('[data-testid^="aviso-nao-lido-"]')).toHaveCount(0);

    await page.getByTestId('avisos-filtro-não-lidos').click();
    await expect(page.locator('[data-testid^="aviso-"]', { hasText: TITULO_LEITURA })).toHaveCount(0);
  });

  test('isolamento por tenant: aviso lido por um mentorado continua não lido pra outro', async ({ page }) => {
    // Marina nunca interagiu com o aviso marcado como lido pelo Carlos no teste acima — se o
    // estado de leitura vazasse entre mentorados, essa asserção pegaria.
    await loginAs(page, 'marina@sabordamarina.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.goto('/mentorado/avisos');

    const cartao = page.locator('[data-testid^="aviso-"]', { hasText: TITULO_LEITURA });
    await expect(cartao).toBeVisible();
    await expect(cartao.locator('[data-testid^="aviso-nao-lido-"]')).toHaveCount(1);
  });

  test('"Marcar todos como lidos" esvazia a aba "Não lidos"', async ({ page }) => {
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.goto('/mentorado/avisos');

    // click() só dispara o evento — não espera o PATCH assíncrono do onClick terminar.
    // waitForResponse garante que a mutação (e o recarregamento subsequente) já aconteceu
    // antes de trocar de aba, evitando uma corrida entre o clique e a rede.
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/mentorado/avisos/marcar-todos-lidos') && res.ok()),
      page.getByTestId('marcar-todos-lidos').click(),
    ]);
    await page.getByTestId('avisos-filtro-não-lidos').click();
    await expect(page.getByText('Nenhum aviso encontrado.')).toBeVisible();
  });

  test('Admin: RBAC — área fora de Conteúdos não acessa a aba de Avisos via URL direta', async ({ page }) => {
    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);

    await page.goto('/admin/conteudos/avisos');
    await expect(page.getByText('Sem acesso')).toBeVisible();
  });
});
