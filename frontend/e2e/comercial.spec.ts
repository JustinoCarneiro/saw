import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('Comercial (E13)', () => {
  test('Formulário público de solicitar acesso (H1.3) cria um lead e mostra confirmação', async ({ page }) => {
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(`Lead Confirmação E2E ${Date.now()}`);
    await page.getByLabel('E-mail').fill(`lead.confirmacao.${Date.now()}@example.com`);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();

    await expect(page.getByText('Solicitação enviada.')).toBeVisible();
    await expect(page.getByText('Nosso time comercial vai entrar em contato em breve.')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/comercial-solicitar-acesso.png' });
  });

  test('Pipeline admin: lead público avança pelo funil até Fechado', async ({ page }) => {
    // O lead nasce pela mesma jornada pública (H1.3) — não via seed — pra este teste cobrir a
    // ponta a ponta real: visitante anônimo -> funil comercial -> venda fechada.
    const nome = `Pipeline E2E ${Date.now()}`;
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(`pipeline.e2e.${Date.now()}@example.com`);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'comercial@sawhub.com.br');
    await page.getByRole('link', { name: 'Comercial' }).click();
    await expect(page).toHaveURL(/\/admin\/comercial\/dashboard$/);

    const main = page.getByRole('main');
    await expect(main.getByText('Novos mentorados no mês')).toBeVisible();

    await page.getByRole('link', { name: 'Funil de vendas' }).click();
    await expect(page).toHaveURL(/\/admin\/comercial\/leads$/);

    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    // exact: true — o nome/e-mail do lead pode conter o nome do status como substring
    // (ex.: "Perdido E2E ..."), então o texto exato do Pill é o único jeito seguro de checar.
    await expect(linha.getByText('Solicitação', { exact: true })).toBeVisible();

    await linha.getByRole('button', { name: 'Mover p/ Em contato' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Em contato', { exact: true })).toBeVisible();

    // Regressão: essa transição (EM_CONTATO -> PROPOSTA) quebrava com 500
    // (LazyInitializationException no vendedor lazy de Lead, achado ao vivo via curl na
    // verificação deste módulo) antes de LeadRepository.buscarPorIdComVendedor existir.
    await linha.getByRole('button', { name: 'Avançar p/ Proposta' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Proposta', { exact: true })).toBeVisible();

    // M25 — "Fechar venda" abre o formulário único de venda (POST .../fechar-venda). M28 — o
    // antigo caminho "Plano fechado" foi removido junto com Plano; fechar venda de verdade
    // sempre passa por aqui agora.
    await linha.getByRole('button', { name: 'Fechar venda' }).click();
    await page.getByLabel('Produto vendido').selectOption({ label: 'Consultoria' });
    await page.getByLabel('Origem da venda').selectOption({ label: 'Direta' });
    await page.getByLabel('Valor total da venda').fill('9000');
    await page.getByLabel('Valor pago no ato').fill('9000');
    await page.getByLabel('Forma de pagamento').selectOption({ label: 'Pix' });
    await page.getByRole('button', { name: 'Confirmar venda' }).click();
    await expect(linha.getByText('Fechado', { exact: true })).toBeVisible();
    await expect(linha.getByText('Consultoria · R$ 9.000,00')).toBeVisible();

    await page.screenshot({ path: 'e2e/screenshots/comercial-funil.png' });
  });

  // Fase 5 (H13.4) — antes desta feature, o único jeito de cadastrar um lead pela área Comercial
  // sem ser o formulário público era importar CSV (M22); não havia como digitar um lead avulso
  // direto na tela (achado relatado pelo cliente ao testar o sistema em produção).
  test('Comercial cria um Lead manualmente, sem passar pelo formulário público', async ({ page }) => {
    const nome = `Lead Manual E2E ${Date.now()}`;
    const email = `lead.manual.${Date.now()}@example.com`;

    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');

    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Criar Lead' }).click();
    await main.getByLabel('Nome').fill(nome);
    await main.getByLabel('E-mail').fill(email);
    await main.getByLabel('Telefone').fill('11988887777');
    await main.getByRole('button', { name: 'Salvar lead' }).click();

    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Solicitação', { exact: true })).toBeVisible();
    await expect(linha.getByText(email)).toBeVisible();
  });

  test('Marcar lead como Perdido exige motivo e aparece na linha', async ({ page }) => {
    const nome = `Perdido E2E ${Date.now()}`;
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(`perdido.e2e.${Date.now()}@example.com`);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');

    const main = page.getByRole('main');
    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Perder' }).click();

    // Sem motivo preenchido, o campo é required (HTML5) — o form não deve submeter.
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(page.getByText(`Marcar "${nome}" como Perdido`)).toBeVisible();

    await page.getByLabel('Motivo').fill('Optou por concorrente (teste E2E)');
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linha.getByText('Perdido', { exact: true })).toBeVisible();
    await expect(linha.getByText('Optou por concorrente (teste E2E)')).toBeVisible();
  });

  test('Ranking mostra meta x realizado do time comercial', async ({ page }) => {
    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/ranking');

    // Escopado pela linha do ranking (testId), não por getByText('Comercial') solto — esse nome
    // colide com o título da seção "Comercial" no menu e com o nome/área do próprio usuário
    // logado no topbar (mesmo texto em 3 lugares da tela, strict-mode violation).
    const main = page.getByRole('main');
    const linha = main.getByTestId('ranking-row').filter({ hasText: 'Comercial' });
    await expect(linha).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/comercial-ranking.png' });
  });

  // Pedido do Marcos (22/07/2026) — "metas e ranking, quem define e onde?": até esta leva,
  // MetaComercial não tinha CRUD nenhum (só o registro seedado por DemoDataSeeder pro Comercial em
  // 2026-07) — qualquer outro vendedor/período ficava invisível no Ranking sem seed manual no
  // banco. Prova o CRUD ponta a ponta: form "+ Definir meta" -> valor certo aparece no ranking.
  // Valor único por execução (não um "4" fixo): a suíte não reseta o banco entre execuções, então
  // reruns encontram a meta de Agosto/2026 já definida por uma execução anterior — um valor fixo
  // não provaria que ESTA chamada de PUT realmente atualizou nada (mesma classe de flake já
  // documentada no LeadRepositoryTest do M05).
  test('Define meta comercial de um vendedor e o valor certo aparece no ranking', async ({ page }) => {
    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/ranking');

    const main = page.getByRole('main');
    await main.getByLabel('Mês').selectOption({ label: 'Agosto' });
    await main.getByLabel('Ano').selectOption('2026');

    const metaValor = String(10 + (Date.now() % 80));
    await main.getByRole('button', { name: 'Definir meta' }).click();
    await main.getByLabel('Vendedor').selectOption({ label: 'Comercial' });
    await main.getByLabel('Meta de fechamentos (vendas)').fill(metaValor);
    await main.getByRole('button', { name: 'Salvar meta' }).click();
    await expect(main.getByRole('button', { name: 'Salvar meta' })).toHaveCount(0);

    // onDefinida dispara 2 GETs (metas + ranking) sem aguardar — o fechamento do form só garante
    // que o PUT terminou, não que o re-fetch já chegou. Timeout maior aqui evita flake sob carga
    // (rodando junto de outros specs) sem mascarar uma falha de verdade.
    const linha = main.getByTestId('ranking-row').filter({ hasText: 'Comercial' });
    await expect(linha).toBeVisible({ timeout: 10000 });
    await expect(linha.getByText(metaValor, { exact: true })).toBeVisible();
  });

  test('RBAC: área fora do Comercial não vê o módulo nem acessa via URL direta', async ({ page }) => {
    await loginAs(page, 'gestao_perf@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await expect(page.getByRole('link', { name: 'Comercial' })).toHaveCount(0);

    await page.goto('/admin/comercial/leads');
    await expect(page.getByText('Sem acesso')).toBeVisible();
  });

  // Change request pós-MVP ("importação de planilhas de eventos passados pra popular histórico",
  // reunião 17/07/2026) — "Live: Tendências do setor" é o evento REALIZADO seedado por
  // DemoDataSeeder (único evento cujo status permite o import histórico).
  test('Importa histórico de vendas de ingresso de um evento já realizado', async ({ page }) => {
    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\/comercial\/dashboard$/);

    const main = page.getByRole('main');
    await expect(main.getByText('Importar histórico de vendas de ingresso')).toBeVisible();
    await main.getByLabel(/Selecione o evento/).selectOption({ label: 'Live: Tendências do setor' });

    const email = `comprador.e2e.${Date.now()}@example.com`;
    const conteudo = 'nomeAluno;quantidadeIngressos;valorLiquidoIngresso;tipoIngresso;origemVenda;email\n'
      + `Comprador Histórico E2E;2;150,00;VIP;HOTMART;${email}`;
    await main.getByTestId('importar-ingressos-input').setInputFiles({
      name: 'vendas-ingresso.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(conteudo, 'utf-8'),
    });

    await expect(main.getByText('2 ingresso(s) importado(s) com sucesso.')).toBeVisible();
  });
});
