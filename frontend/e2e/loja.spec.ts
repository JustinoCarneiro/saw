import { expect, test } from '@playwright/test';
import { csrfHeaders, loginAs } from './helpers';
import { assinaturaWebhook, registrarPagamentoNoStub } from './mercadopago';

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

  // Antes do stub do Mercado Pago, este teste só provava o caminho "gateway não configurado"
  // (503) — coberto à parte no unitário (MercadoPagoGatewayServiceTest,
  // semAccessTokenConfiguradoCriarPreferenciaLancaPagamentoIndisponivel), então essa cobertura
  // não se perde. Com o gateway configurado no E2E (stub, ver scripts/e2e-mercadopago-stub-server.mjs),
  // vale mais testar o que ninguém testava: um pagamento recusado NÃO libera o pedido, e uma
  // nova notificação aprovada (retry, comum no gateway real) libera de verdade — H8.3.
  test('checkout com gateway configurado cria preferência; pagamento recusado não libera, aprovado depois libera', async ({ page, request }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const carrinhoInicial = await page.request.get('/api/v1/mentorado/loja/carrinho');
    const { itens: itensIniciais }: { itens: { id: string }[] } = await carrinhoInicial.json();
    for (const item of itensIniciais) {
      await page.request.delete(`/api/v1/mentorado/loja/carrinho/itens/${item.id}`, { headers: await csrfHeaders(page) });
    }

    await page.getByRole('link', { name: 'Loja SAW' }).click();
    const template = page.locator('[data-testid^="produto-"]', { hasText: 'Template de Cardápio Digital' });
    await template.getByRole('button', { name: 'Adicionar ao carrinho' }).click();

    // Espera o item aparecer no carrinho antes de seguir: o clique só garante que o handler
    // disparou, não que o POST assíncrono terminou — sem isso, o checkout (chamado direto via
    // API logo abaixo) corre e acha carrinho vazio (409) por pura corrida.
    await page.getByTestId('loja-tab-CARRINHO').click();
    await expect(page.locator('[data-testid^="item-carrinho-"]', { hasText: 'Template de Cardápio Digital' })).toBeVisible();

    // Checkout via API direto (não clica no botão da UI): o clique real navegaria o browser pro
    // checkoutUrl devolvido pelo stub — uma URL falsa, sem página de verdade por trás. A chamada
    // HTTP com o Mercado Pago (criarPreferencia) já roda de verdade aqui dentro do backend, mesmo
    // sem passar pelo clique. page.request não tem o interceptor de CSRF do apiClient (axios) —
    // header manual via csrfHeaders (ver helpers.ts), senão 403.
    const checkoutRes = await page.request.post('/api/v1/mentorado/loja/checkout', { headers: await csrfHeaders(page) });
    expect(checkoutRes.ok()).toBe(true);
    expect((await checkoutRes.json()).checkoutUrl).toContain('stub-checkout.invalid');

    const pedidosRes = await page.request.get('/api/v1/mentorado/loja/pedidos');
    const pedidos: { id: string; status: string }[] = await pedidosRes.json();
    const pedido = pedidos.find((p) => p.status === 'AGUARDANDO_PAGAMENTO');
    expect(pedido).toBeTruthy();

    // O SAW HUB nunca confia no corpo da notificação do webhook (só no id) — sempre re-consulta
    // aqui no stub, mesmo padrão de "verdade" que consultaria a API real do Mercado Pago.
    async function notificar(status: string) {
      const paymentId = `stub-payment-${pedido!.id}`;
      await registrarPagamentoNoStub(request, paymentId, status, pedido!.id);
      const xRequestId = `stub-request-${Date.now()}-${status}`;
      const { xSignature } = assinaturaWebhook(paymentId, xRequestId);
      const res = await page.request.post(
        `/api/v1/webhooks/mercadopago?data.id=${paymentId}&type=payment`,
        { headers: { 'x-signature': xSignature, 'x-request-id': xRequestId } },
      );
      expect(res.ok()).toBe(true);
    }

    // 1) Recusado: pedido continua aguardando, carrinho não se perde (H8.3).
    await notificar('rejected');
    const aposRecusa: { id: string; status: string }[] = await (await page.request.get('/api/v1/mentorado/loja/pedidos')).json();
    expect(aposRecusa.find((p) => p.id === pedido!.id)?.status).toBe('AGUARDANDO_PAGAMENTO');

    // 2) Nova tentativa aprovada (mesmo pedido, gateway real notifica de novo em retries) — libera.
    await notificar('approved');
    await page.getByTestId('loja-tab-PEDIDOS').click();
    const linhaPedido = page.getByTestId(`pedido-${pedido!.id}`);
    await expect(linhaPedido.getByText('Liberado')).toBeVisible();
  });

  test('webhook com assinatura inválida é rejeitado (403), pedido não é liberado', async ({ page }) => {
    await loginAs(page, 'rafael@bistrogomes.com.br');
    const res = await page.request.post(
      '/api/v1/webhooks/mercadopago?data.id=qualquer&type=payment',
      { headers: { 'x-signature': 'ts=123,v1=assinatura-forjada', 'x-request-id': 'req-forjado' } },
    );
    expect(res.status()).toBe(403);
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
    await loginAs(page, 'admin@sawhub.com.br');
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

  // Achado ao vivo (print do cliente): "Cancelar"/"Reembolsar" em Comercial → Loja — Pedidos
  // devolvia "Não foi possível concluir a ação." (500) pra TODO pedido — buscarPorIdComItens()
  // não fazia FETCH JOIN em mentorado (@ManyToOne LAZY), e PedidoAdminResponse.from() lê
  // pedido.getMentorado() no controller, já fora da transação do service. Corrigido no
  // PedidoRepository (ver PedidoRepositoryTest, unit). Nenhum dos dois fluxos tinha E2E até aqui.
  test('Admin cancela um pedido Aguardando pagamento', async ({ page }) => {
    await loginAs(page, 'marina@sabordamarina.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const carrinhoInicial = await page.request.get('/api/v1/mentorado/loja/carrinho');
    const { itens: itensIniciais }: { itens: { id: string }[] } = await carrinhoInicial.json();
    for (const item of itensIniciais) {
      await page.request.delete(`/api/v1/mentorado/loja/carrinho/itens/${item.id}`, { headers: await csrfHeaders(page) });
    }

    await page.getByRole('link', { name: 'Loja SAW' }).click();
    const ebook = page.locator('[data-testid^="produto-"]', { hasText: 'E-book: Gestão de Custos' });
    await ebook.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
    await page.getByTestId('loja-tab-CARRINHO').click();
    await expect(page.locator('[data-testid^="item-carrinho-"]', { hasText: 'E-book: Gestão de Custos' })).toBeVisible();

    const checkoutRes = await page.request.post('/api/v1/mentorado/loja/checkout', { headers: await csrfHeaders(page) });
    expect(checkoutRes.ok()).toBe(true);
    const pedidosRes = await page.request.get('/api/v1/mentorado/loja/pedidos');
    const pedidos: { id: string; status: string }[] = await pedidosRes.json();
    const pedido = pedidos.find((p) => p.status === 'AGUARDANDO_PAGAMENTO');
    expect(pedido).toBeTruthy();

    await page.context().clearCookies();
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);
    await page.goto('/admin/comercial/pedidos');

    const linhaPedido = page.getByTestId(`pedido-row-${pedido!.id}`);
    await expect(linhaPedido).toBeVisible();
    await linhaPedido.getByRole('button', { name: 'Cancelar' }).click();
    await page.getByRole('button', { name: 'Cancelar pedido' }).click();

    await expect(linhaPedido.getByText('Cancelado')).toBeVisible();
    await expect(page.getByText('Não foi possível concluir a ação.')).toHaveCount(0);
  });

  test('Admin reembolsa um pedido Liberado', async ({ page, request }) => {
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const carrinhoInicial = await page.request.get('/api/v1/mentorado/loja/carrinho');
    const { itens: itensIniciais }: { itens: { id: string }[] } = await carrinhoInicial.json();
    for (const item of itensIniciais) {
      await page.request.delete(`/api/v1/mentorado/loja/carrinho/itens/${item.id}`, { headers: await csrfHeaders(page) });
    }

    await page.getByRole('link', { name: 'Loja SAW' }).click();
    const pacote = page.locator('[data-testid^="produto-"]', { hasText: 'Pacote de Planilhas Gerenciais' });
    await pacote.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
    await page.getByTestId('loja-tab-CARRINHO').click();
    await expect(page.locator('[data-testid^="item-carrinho-"]', { hasText: 'Pacote de Planilhas Gerenciais' })).toBeVisible();

    const checkoutRes = await page.request.post('/api/v1/mentorado/loja/checkout', { headers: await csrfHeaders(page) });
    expect(checkoutRes.ok()).toBe(true);
    const pedidosRes = await page.request.get('/api/v1/mentorado/loja/pedidos');
    const pedidos: { id: string; status: string }[] = await pedidosRes.json();
    const pedido = pedidos.find((p) => p.status === 'AGUARDANDO_PAGAMENTO');
    expect(pedido).toBeTruthy();

    const paymentId = `stub-payment-reembolso-${pedido!.id}`;
    await registrarPagamentoNoStub(request, paymentId, 'approved', pedido!.id);
    const xRequestId = `stub-request-reembolso-${Date.now()}`;
    const { xSignature } = assinaturaWebhook(paymentId, xRequestId);
    const webhookRes = await page.request.post(
      `/api/v1/webhooks/mercadopago?data.id=${paymentId}&type=payment`,
      { headers: { 'x-signature': xSignature, 'x-request-id': xRequestId } },
    );
    expect(webhookRes.ok()).toBe(true);

    await page.context().clearCookies();
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);
    await page.goto('/admin/comercial/pedidos');

    const linhaPedido = page.getByTestId(`pedido-row-${pedido!.id}`);
    await expect(linhaPedido).toBeVisible();
    await expect(linhaPedido.getByText('Liberado')).toBeVisible();
    await linhaPedido.getByRole('button', { name: 'Reembolsar' }).click();
    await page.getByRole('button', { name: 'Confirmar reembolso' }).click();

    await expect(linhaPedido.getByText('Reembolsado')).toBeVisible();
    await expect(page.getByText('Não foi possível concluir a ação.')).toHaveCount(0);
  });
});
