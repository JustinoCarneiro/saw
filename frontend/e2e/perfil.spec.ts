import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M15 — E9 Perfil & Gamificação', () => {
  test('mentorado vê identidade, jornada e assinatura no próprio perfil', async ({ page }) => {
    await loginAs(page, 'ana@anacosta.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Perfil' }).click();
    await expect(page).toHaveURL(/\/mentorado\/perfil/);

    const cartao = page.getByTestId('perfil-cartao');
    await expect(cartao.getByText('Ana Costa', { exact: true })).toBeVisible();
    await expect(cartao.getByText('Cantina Ana Costa')).toBeVisible();
    await expect(cartao.getByText('ana@anacosta.com.br')).toBeVisible();

    const jornada = page.getByTestId('jornada-cartao');
    await expect(jornada.getByText('Nível atual:')).toBeVisible();
    await expect(jornada.getByTestId('conquista-MENTORIA_REALIZADA')).toHaveClass(/conquistaDesbloqueada/);
    await expect(jornada.getByTestId('conquista-MARATONISTA')).toHaveClass(/conquistaBloqueada/);
    // H9.2 — já era verdadeira no seed, antes do rastreamento de data existir (V18): a primeira
    // visita a este perfil tem que mostrar "Desde sempre", nunca a data de hoje fabricada.
    await expect(jornada.getByTestId('conquista-MENTORIA_REALIZADA').getByText('Desde sempre')).toBeVisible();

    const assinatura = page.getByTestId('assinatura-cartao');
    await expect(assinatura.getByText('Essencial')).toBeVisible();
    await expect(assinatura.getByText('20/09/2026')).toBeVisible();
    await expect(assinatura.getByText('Profissional')).toBeVisible(); // única opção acima de Essencial
  });

  test('H9.2 — conquista desbloqueada depois da primeira visita ao perfil ganha data real, não "Desde sempre"', async ({ page }) => {
    // Rafael não tem nenhuma meta no seed e nenhum outro spec cria uma pra ele (ao contrário de
    // Carlos, que metas.spec.ts usa como fixture de "zero metas" — criar uma aqui quebraria
    // aquele teste numa segunda execução contra o mesmo banco) — META_BATIDA parte de false,
    // então dá pra observar as duas fases: primeira visita (estabelece o marco de "já
    // observamos esse mentorado"), depois desbloqueio real.
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Perfil' }).click();
    const metaBatida = page.getByTestId('jornada-cartao').getByTestId('conquista-META_BATIDA');
    await expect(metaBatida).toHaveClass(/conquistaBloqueada/);

    const titulo = `Meta E2E ${Date.now()}`;
    await page.getByRole('link', { name: 'Metas' }).click();
    await page.getByRole('button', { name: 'Nova meta' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Prazo').fill('2026-12-31');
    await page.getByRole('button', { name: 'Criar meta' }).click();
    const linha = page.locator('[data-testid^="meta-row-"]', { hasText: titulo });
    await expect(linha).toBeVisible();
    await linha.getByRole('button', { name: 'Concluir' }).click();
    await expect(page.getByText(titulo)).toHaveCount(0);

    await page.getByRole('link', { name: 'Perfil' }).click();
    await expect(metaBatida).toHaveClass(/conquistaDesbloqueada/);
    await expect(metaBatida.getByText('Desde sempre')).toHaveCount(0);
    // Formato dd/mm/aaaa — se apareceu, é uma data real (hoje), não o sentinel "desde sempre".
    await expect(metaBatida.getByText(/\d{2}\/\d{2}\/\d{4}/)).toBeVisible();
  });

  test('mentorado edita telefone/bio/áreas/foto e vê o cartão atualizado', async ({ page }) => {
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Perfil' }).click();

    await page.getByTestId('editar-perfil').click();
    const form = page.getByTestId('perfil-form');
    await form.getByLabel('Telefone').fill('(31) 90000-1111');
    await form.getByLabel('Sobre mim').fill('Bio editada pelo teste E2E.');
    await form.getByLabel(/Áreas de interesse/).fill('Marketing, Processos, Gestão');
    await form.getByLabel(/Foto de perfil/).fill('https://cdn.sawhub.com.br/fernanda.jpg');
    await form.getByRole('button', { name: 'Salvar' }).click();

    const cartao = page.getByTestId('perfil-cartao');
    await expect(cartao.getByText('(31) 90000-1111')).toBeVisible();
    await expect(cartao.getByText('Bio editada pelo teste E2E.')).toBeVisible();
    await expect(cartao.getByText('Marketing')).toBeVisible();
    await expect(cartao.getByText('Gestão')).toBeVisible();

    // PATCH grava valor absoluto (não soma) — reexecuções do teste são idempotentes, sem
    // necessidade de limpeza (diferente do carrinho da Loja/M14, que é uma ação aditiva).
  });

  test('editar perfil não altera nome, negócio nem plano (admin-only)', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Perfil' }).click();

    await page.getByTestId('editar-perfil').click();
    const form = page.getByTestId('perfil-form');
    await form.getByLabel('Telefone').fill('(21) 91111-2222');
    await form.getByRole('button', { name: 'Salvar' }).click();

    const cartao = page.getByTestId('perfil-cartao');
    await expect(cartao.getByText('Rafael Gomes')).toBeVisible();
    await expect(cartao.getByText('Bistrô Gomes')).toBeVisible();
    const assinatura = page.getByTestId('assinatura-cartao');
    await expect(assinatura.getByText('Essencial')).toBeVisible();
  });

  test('isolamento por tenant: perfil de um mentorado não vaza pra outro', async ({ page }) => {
    await loginAs(page, 'marina@sabordamarina.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Perfil' }).click();

    const cartao = page.getByTestId('perfil-cartao');
    await expect(cartao.getByText('Marina Souza')).toBeVisible();
    await expect(cartao.getByText('ana@anacosta.com.br')).toHaveCount(0);
    await expect(page.getByText('Focada em padronizar a cozinha')).toHaveCount(0);
  });

  test('Admin: editar vencimento do plano reflete no perfil do mentorado', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);

    await page.goto('/admin/mentorados/lista');
    const linha = page.locator('[data-testid^="mentorado-row-"]', { hasText: 'Carlos Menezes' });
    await expect(linha).toBeVisible();
    await linha.getByRole('button', { name: 'Editar' }).click();

    await page.getByLabel('Vencimento do plano').fill('2026-12-25');
    await page.getByRole('button', { name: 'Salvar' }).click();
    await expect(page.getByText('Editar mentorado')).toHaveCount(0);

    // Confirma via API (view-only, sessão de Admin — não dá pra logar como Carlos na mesma aba).
    const res = await page.request.get('/api/v1/admin/mentorados', { params: { busca: 'Carlos Menezes' } });
    const [carlos] = await res.json();
    expect(carlos.vencimentoPlano).toBe('2026-12-25');
  });
});
