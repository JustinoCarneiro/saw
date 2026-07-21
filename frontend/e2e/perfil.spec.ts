import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M15 — E9 Perfil & Gamificação', () => {
  test('mentorado vê identidade, jornada e tipo de contrato no próprio perfil', async ({ page }) => {
    await loginAs(page, 'ana@anacosta.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Perfil' }).click();
    await expect(page).toHaveURL(/\/mentorado\/perfil/);

    const cartao = page.getByTestId('perfil-cartao');
    await expect(cartao.getByText('Ana Costa', { exact: true })).toBeVisible();
    await expect(cartao.getByText('Cantina Ana Costa')).toBeVisible();
    await expect(cartao.getByText('ana@anacosta.com.br')).toBeVisible();
    // M28 — "Plano atual"/upgrade removido ("não existem planos, mas sim produtos"); Tipo de
    // contrato passou a ser só informativo dentro do próprio cartão de perfil.
    await expect(cartao.getByText('Mentoria Individual')).toBeVisible();

    const jornada = page.getByTestId('jornada-cartao');
    await expect(jornada.getByText('Nível atual:')).toBeVisible();
    await expect(jornada.getByTestId('conquista-MENTORIA_REALIZADA')).toHaveClass(/conquistaDesbloqueada/);
    await expect(jornada.getByTestId('conquista-MARATONISTA')).toHaveClass(/conquistaBloqueada/);
    // H9.2 — já era verdadeira no seed, antes do rastreamento de data existir (V18): a primeira
    // visita a este perfil tem que mostrar "Desde sempre", nunca a data de hoje fabricada.
    await expect(jornada.getByTestId('conquista-MENTORIA_REALIZADA').getByText('Desde sempre')).toBeVisible();
  });

  test('H9.2 — conquista desbloqueada depois da primeira visita ao perfil ganha data real, não "Desde sempre"', async ({ page }) => {
    // META_BATIDA é permanente uma vez desbloqueada (semântica correta de gamificação — não há
    // "rebloquear"), e o banco de E2E persiste entre execuções (e2e-up.sh só cria o banco se não
    // existir, nunca recria do zero). Reusar um mentorado fixo (ex.: Rafael) contamina esse teste
    // pra sempre depois da primeira vez que ele passa: na 2ª execução, META_BATIDA já nasce
    // desbloqueada e a asserção de precondição abaixo falha. Por isso cria um mentorado
    // descartável a cada execução, pelo mesmo fluxo público lead -> Fechado -> "criar a partir de
    // um lead" já usado em mentorados.spec.ts — garante zero histórico sempre.
    const timestamp = Date.now();
    const nome = `Perfil H9.2 E2E ${timestamp}`;
    const email = `h92.${timestamp}@example.com`;

    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');
    const main = page.getByRole('main');
    const linhaLead = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linhaLead.getByRole('button', { name: 'Mover p/ Em contato' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await linhaLead.getByRole('button', { name: 'Avançar p/ Proposta' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await linhaLead.getByRole('button', { name: 'Fechar venda' }).click();
    await page.getByLabel('Produto vendido').selectOption({ label: 'Mentoria contínua' });
    await page.getByLabel('Origem da venda').selectOption({ label: 'Direta' });
    await page.getByLabel('Valor total da venda').fill('26000');
    await page.getByLabel('Valor pago no ato').fill('6000');
    await page.getByLabel('Forma de pagamento').selectOption({ label: 'Pix' });
    await page.getByRole('button', { name: 'Confirmar venda' }).click();
    await expect(linhaLead.getByText('Fechado', { exact: true })).toBeVisible();

    // LoginPage redireciona pra /admin se já houver sessão ativa — precisa limpar cookies antes
    // de cada novo login, senão o formulário nunca aparece (mesmo cuidado do mentorados.spec.ts).
    await page.context().clearCookies();
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');
    await main.getByRole('button', { name: 'Criar a partir de um lead' }).click();
    await page.getByLabel('Lead').selectOption({ label: `${nome} — ${email}` });
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    const senhaTemporariaRaw = await page.locator('code').textContent();
    if (!senhaTemporariaRaw) throw new Error('Senha temporária não encontrada na tela de mentorado criado.');
    const senhaTemporaria = senhaTemporariaRaw.trim();
    await page.getByRole('button', { name: 'Entendi' }).click();

    await page.context().clearCookies();
    await loginAs(page, email, senhaTemporaria);
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

  // areasInteresse removido no M23 (change request pós-MVP, confirmado pelo cliente como não
  // aplicável) — antes cobria telefone/bio/áreas/foto, agora só telefone/bio/foto.
  test('mentorado edita telefone/bio/foto e vê o cartão atualizado', async ({ page }) => {
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Perfil' }).click();

    await page.getByTestId('editar-perfil').click();
    const form = page.getByTestId('perfil-form');
    await form.getByLabel('Telefone').fill('(31) 90000-1111');
    await form.getByLabel('Sobre mim').fill('Bio editada pelo teste E2E.');
    await form.getByLabel(/Foto de perfil/).fill('https://cdn.sawhub.com.br/fernanda.jpg');
    await form.getByRole('button', { name: 'Salvar' }).click();

    const cartao = page.getByTestId('perfil-cartao');
    await expect(cartao.getByText('(31) 90000-1111')).toBeVisible();
    await expect(cartao.getByText('Bio editada pelo teste E2E.')).toBeVisible();

    // PATCH grava valor absoluto (não soma) — reexecuções do teste são idempotentes, sem
    // necessidade de limpeza (diferente do carrinho da Loja/M14, que é uma ação aditiva).
  });

  test('editar perfil não altera nome, negócio nem tipo de contrato (admin-only)', async ({ page }) => {
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
    await expect(cartao.getByText('Mentoria Individual')).toBeVisible();
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
    // M28 ("página dedicada de mentorado") — "Editar" virou "Ver perfil" e navega pra uma página
    // própria em vez de expandir um form inline nesta tela.
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    await page.getByLabel('Vencimento do plano').fill('2026-12-25');
    await page.getByRole('button', { name: 'Salvar', exact: true }).click();
    await expect(page.getByText('Salvo.')).toBeVisible();

    // Confirma via API (view-only, sessão de Admin — não dá pra logar como Carlos na mesma aba).
    const res = await page.request.get('/api/v1/admin/mentorados', { params: { busca: 'Carlos Menezes' } });
    const [carlos] = await res.json();
    expect(carlos.vencimentoPlano).toBe('2026-12-25');
  });
});
