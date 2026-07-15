import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M14 — E8 Loja SAW (catálogo, carrinho, checkout)', () => {
  test('mentorado navega o catálogo, filtra por categoria e vê destaque/desconto', async ({ page }) => {
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Loja SAW' }).click();
    await expect(page).toHaveURL(/\/mentorado\/loja/);

    const kit = page.locator('[data-testid^="produto-"]', { hasText: 'Kit Abertura de Restaurante' });
    await expect(kit).toBeVisible();
    await expect(kit.getByText('DESTAQUE')).toBeVisible();
    await expect(kit.getByText('R$ 497,00')).toBeVisible(); // preço original riscado

    // Produto não publicado (Consultoria Express) nunca aparece
    await expect(page.getByText('Consultoria Express')).toHaveCount(0);
  });

  test('mentorado adiciona ao carrinho, ajusta quantidade e vê o subtotal atualizar', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    // "Adicionar ao carrinho" SOMA quantidade se o item já existir (não reseta) — uma execução
    // anterior que falhou antes do cleanup deixaria quantidade residual e quebraria a asserção
    // de valor absoluto abaixo. Começa sempre de um carrinho vazio via API, não via UI.
    const carrinhoInicial = await page.request.get('/api/v1/mentorado/loja/carrinho');
    const { itens: itensIniciais }: { itens: { id: string }[] } = await carrinhoInicial.json();
    for (const item of itensIniciais) {
      await page.request.delete(`/api/v1/mentorado/loja/carrinho/itens/${item.id}`);
    }

    await page.getByRole('link', { name: 'Loja SAW' }).click();

    const pacote = page.locator('[data-testid^="produto-"]', { hasText: 'Pacote de Planilhas Gerenciais' });
    await pacote.getByRole('button', { name: 'Adicionar ao carrinho' }).click();

    await page.getByTestId('loja-tab-CARRINHO').click();
    const item = page.locator('[data-testid^="item-carrinho-"]', { hasText: 'Pacote de Planilhas Gerenciais' });
    await expect(item).toBeVisible();
    // Sem exact:true: Intl.NumberFormat('pt-BR') usa espaço não separável ( ) entre "R$" e o
    // valor — getByText normaliza espaço em branco em modo substring, não em modo exato.
    // .first(): "R$ 97,00" aparece 2x na linha ("... cada" + subtotal, mesmo valor c/ quantidade=1).
    await expect(item.getByText('R$ 97,00').first()).toBeVisible();

    await item.getByRole('button', { name: '+' }).click();
    await expect(item.getByText('R$ 194,00')).toBeVisible();

    // Limpa o carrinho pra não poluir a próxima execução (item.getByRole 'Remover')
    await item.getByRole('button', { name: 'Remover' }).click();
    await expect(page.getByText('Seu carrinho está vazio.')).toBeVisible();
  });

  test('produto so-unidade nao deixa passar de 1 no carrinho', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const carrinhoInicial = await page.request.get('/api/v1/mentorado/loja/carrinho');
    const { itens: itensIniciais }: { itens: { id: string }[] } = await carrinhoInicial.json();
    for (const item of itensIniciais) {
      await page.request.delete(`/api/v1/mentorado/loja/carrinho/itens/${item.id}`);
    }

    await page.getByRole('link', { name: 'Loja SAW' }).click();

    const ebook = page.locator('[data-testid^="produto-"]', { hasText: 'E-book: Gestão de Custos' });
    await ebook.getByRole('button', { name: 'Adicionar ao carrinho' }).click();

    await page.getByTestId('loja-tab-CARRINHO').click();
    const item = page.locator('[data-testid^="item-carrinho-"]', { hasText: 'E-book: Gestão de Custos' });
    // Reforço no servidor (Pedido.exigirQuantidadePermitida) coberto por PedidoTest (unit) —
    // aqui o foco é o comportamento visível: o "+" trava em 1, sem tentar contornar via API crua.
    await expect(item.getByRole('button', { name: '+' })).toBeDisabled();

    await item.getByRole('button', { name: 'Remover' }).click();
    await expect(page.getByText('Seu carrinho está vazio.')).toBeVisible();
  });

  test('checkout sem credencial do gateway falha limpo, sem quebrar a tela', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    // Mesma limpeza defensiva do teste acima — garante carrinho vazio antes de começar.
    const carrinhoInicial = await page.request.get('/api/v1/mentorado/loja/carrinho');
    const { itens: itensIniciais }: { itens: { id: string }[] } = await carrinhoInicial.json();
    for (const item of itensIniciais) {
      await page.request.delete(`/api/v1/mentorado/loja/carrinho/itens/${item.id}`);
    }

    await page.getByRole('link', { name: 'Loja SAW' }).click();

    const template = page.locator('[data-testid^="produto-"]', { hasText: 'Template de Cardápio Digital' });
    await template.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
    await page.getByTestId('loja-tab-CARRINHO').click();

    await page.getByTestId('finalizar-compra').click();
    // Ambiente de dev não tem MERCADOPAGO_ACCESS_TOKEN — erro claro (503, ver
    // GlobalExceptionHandler.handlePagamentoIndisponivel), não uma tela quebrada, e o carrinho
    // continua visível (não navega pra lugar nenhum, ver LojaMentoradoService.checkout).
    await expect(page.getByTestId('loja-erro')).toBeVisible();
    await expect(page).toHaveURL(/\/mentorado\/loja/);

    // Limpa o item adicionado nesta execução.
    const item = page.locator('[data-testid^="item-carrinho-"]', { hasText: 'Template de Cardápio Digital' });
    await item.getByRole('button', { name: 'Remover' }).click();
  });

  test('mentorado vê "Meus Pedidos" com o pedido liberado (seed) e o link de download', async ({ page }) => {
    await loginAs(page, 'ana@anacosta.com.br');
    await expect(page).toHaveURL(/\/mentorado/);
    await page.getByRole('link', { name: 'Loja SAW' }).click();

    await page.getByTestId('loja-tab-PEDIDOS').click();
    const pedido = page.locator('[data-testid^="pedido-"]', { hasText: 'Template de Cardápio Digital' });
    await expect(pedido).toBeVisible();
    await expect(pedido.getByText('Liberado')).toBeVisible();
    await expect(pedido.getByRole('link', { name: 'Baixar' })).toHaveAttribute('href', /cdn\.sawhub\.com\.br/);
  });

  test('isolamento por tenant: carrinho de um mentorado não aparece pra outro', async ({ page }) => {
    // Marina nunca mexeu no carrinho nesta leva — se o carrinho de Rafael (testes acima) vazasse,
    // essa asserção pegaria.
    await loginAs(page, 'marina@sabordamarina.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const res = await page.request.get('/api/v1/mentorado/loja/carrinho');
    expect(res.status()).toBe(200);
    const carrinho = await res.json();
    expect(carrinho.itens).toHaveLength(0);
  });

  test('Admin: criar produto nasce Rascunho e Publicar muda o status', async ({ page }) => {
    // Verificação de que Rascunho nunca vaza pro catálogo do mentorado já é coberta por
    // ProdutoServiceTest/LojaMentoradoServiceTest (unit) e por curl ao vivo (Consultoria Express
    // seedada, nunca publicada) — aqui o foco é só a UI/wiring do CRUD do Admin.
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);

    await page.goto('/admin/comercial/produtos');
    const titulo = `Produto E2E ${Date.now()}`;
    await page.getByRole('button', { name: '+ Novo produto' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Descrição').fill('Produto criado pelo teste E2E.');
    await page.getByLabel(/Preço \(R\$\)/).fill('19.90');
    await page.getByLabel('Link do arquivo (liberado ao mentorado após pagamento)').fill('https://cdn.sawhub.com.br/e2e.pdf');
    await page.getByRole('button', { name: 'Salvar' }).click();

    const linha = page.locator('[data-testid^="produto-row-"]', { hasText: titulo });
    await expect(linha).toBeVisible();
    await expect(linha.getByText('Rascunho')).toBeVisible();

    await linha.getByRole('button', { name: 'Publicar' }).click();
    await expect(linha.getByText('Publicado')).toBeVisible();
  });
});
