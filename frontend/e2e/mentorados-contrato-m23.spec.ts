import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

// M23 (change request pós-MVP, reunião 17/07/2026, ver ROADMAP.md § Blueprint M23) — "criar
// mentorado direto" + dados de contrato + Diagnóstico Inicial. /direto e /dados-contrato nasceram
// atrás de Modulo.COMERCIAL (achado do revisor-seguranca: CNPJ/sócios/valor de contrato são dado
// comercial sensível). Pedido do Marcos (22/07/2026) reverteu essa restrição pra Gestão de
// Performance: o gate agora aceita COMERCIAL OU MENTORADOS (ver RequiresModulo em
// MentoradoContratoController), então tanto Fundador quanto Gestão de Performance veem os botões.
test.describe('M23 — Mentorado: criar direto, dados de contrato, Diagnóstico Inicial', () => {
  test('Fundador cria mentorado direto e edita os dados de contrato', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const timestamp = Date.now();
    const nome = `Mentorado Direto E2E ${timestamp}`;
    const email = `direto.${timestamp}@example.com`;
    const main = page.getByRole('main');

    await main.getByRole('button', { name: 'Criar mentorado direto' }).click();
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Negócio').fill('Restaurante E2E');
    await page.getByLabel('Tipo de contrato').selectOption({ label: 'Mentoria Contínua' });
    await page.getByLabel('Valor do contrato (R$)').fill('26000');
    await page.getByLabel('Data de fechamento').fill('2026-07-17');
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();

    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await expect(page.getByText('Senha temporária:')).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    // Nasceu com o tipo de contrato já setado na criação — confere que persistiu antes de editar
    // os outros campos de contrato (CNPJ/sócios/nome fantasia não fazem parte do form de criação
    // direta, só de "dados de contrato").
    // M28 ("página dedicada de mentorado") — "Editar" virou "Ver perfil" e navega pra uma página
    // própria (/admin/mentorados/lista/:id) em vez de expandir um form inline nesta tela.
    const linha = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);
    await expect(page.getByText('Dados de contrato', { exact: true })).toBeVisible();
    await expect(page.getByLabel('Tipo de contrato')).toHaveValue('MENTORIA_CONTINUA');
    await expect(page.getByText('Vencimento calculado: 2027-07-17')).toBeVisible();

    await page.getByLabel('Nome fantasia').fill('Restaurante da Maria E2E');
    // Auditoria de UX (22/07/2026) — pressSequentially (não .fill) digita dígito a dígito, pra
    // provar que a máscara formata em tempo real, não só quando o valor já chega pronto.
    await page.getByLabel('CNPJ').pressSequentially('42521899000138');
    await expect(page.getByLabel('CNPJ')).toHaveValue('42.521.899/0001-38');
    await page.getByLabel('Sócios').fill('Maria Silva; João Silva');
    await page.getByRole('button', { name: 'Salvar dados de contrato' }).click();
    await expect(page.getByText('Salvo.')).toBeVisible();

    // Recarrega a página (GET /admin/mentorados/{id}) pra confirmar que persistiu de verdade, não
    // só o estado local do form.
    await page.reload();
    await expect(page.getByLabel('Nome fantasia')).toHaveValue('Restaurante da Maria E2E');
    await expect(page.getByLabel('CNPJ')).toHaveValue('42.521.899/0001-38');
    await expect(page.getByLabel('Sócios')).toHaveValue('Maria Silva; João Silva');

    // Upload/download do PDF do contrato — endpoint separado (multipart), não faz parte do
    // submit dos campos de texto (ver ContratoDocumentoStorageService no backend).
    await expect(page.getByRole('button', { name: 'Baixar contrato atual' })).toHaveCount(0);
    await page.getByLabel('Documento do contrato (PDF)').setInputFiles({
      name: 'contrato.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4 conteudo-fake-e2e'),
    });
    await page.getByRole('button', { name: 'Enviar documento' }).click();
    await expect(page.getByRole('button', { name: 'Baixar contrato atual' })).toBeVisible();

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Baixar contrato atual' }).click(),
    ]);
    expect(download.suggestedFilename()).toBe('contrato.pdf');
  });

  // Auditoria de UX (22/07/2026) — achado real: GlobalExceptionHandler não tinha handler pra
  // MethodArgumentNotValidException, então uma falha de @Valid (@Pattern do CNPJ) caía no
  // ProblemDetail padrão do Spring sem o campo "message" que getApiErrorMessage usa — o usuário
  // via sempre o fallback genérico da tela, nunca a mensagem específica. CNPJ incompleto (a
  // máscara nunca produz um valor com o formato errado, só incompleto) é o jeito real de
  // reproduzir isso pela UI.
  test('CNPJ incompleto mostra a mensagem específica de formato, não o fallback genérico', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const main = page.getByRole('main');
    const linha = main.locator('text=Carlos Menezes').locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    await page.getByLabel('CNPJ').pressSequentially('123');
    await expect(page.getByLabel('CNPJ')).toHaveValue('12.3');
    await page.getByRole('button', { name: 'Salvar dados de contrato' }).click();

    await expect(page.getByText('CNPJ deve estar no formato 00.000.000/0000-00 ou 14 dígitos')).toBeVisible();
    await expect(page.getByText('Não foi possível salvar os dados de contrato. Tente novamente.')).toHaveCount(0);
  });

  // Léa ("Sucesso do Gestor") é quem preenche o Diagnóstico Inicial na operação real (ver
  // Fluxograma.pdf citado em docs/reuniao-2026-07-17-atualizacoes.md) — mapeia pra área Gestão de
  // Performance no sistema. Pedido do Marcos (22/07/2026) — "acesso pleno" — deu a ela paridade
  // com o Fundador nesta área: cria mentorado direto, edita dados de contrato, além do
  // Diagnóstico Inicial que já preenchia antes.
  test('Gestão de Performance cria mentorado direto, edita dados de contrato e preenche o Diagnóstico Inicial', async ({ page }) => {
    await loginAs(page, 'gestao_perf@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    const timestamp = Date.now();
    const nome = `Mentorado Direto GP E2E ${timestamp}`;
    const email = `direto.gp.${timestamp}@example.com`;
    const main = page.getByRole('main');

    await expect(main.getByRole('button', { name: 'Criar mentorado direto' })).toBeVisible();
    await expect(main.getByRole('button', { name: 'Importar mentorados (CSV)' })).toBeVisible();
    await expect(main.getByRole('button', { name: 'Criar a partir de um lead' })).toBeVisible();

    await main.getByRole('button', { name: 'Criar mentorado direto' }).click();
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Negócio').fill('Restaurante GP E2E');
    await page.getByLabel('Tipo de contrato').selectOption({ label: 'Mentoria Contínua' });
    await page.getByLabel('Valor do contrato (R$)').fill('18000');
    await page.getByLabel('Data de fechamento').fill('2026-07-22');
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();

    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    const linhaCriado = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linhaCriado.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);
    await expect(page.getByText('Dados de contrato', { exact: true })).toBeVisible();

    await page.getByLabel('Nome fantasia').fill('Restaurante GP E2E Fantasia');
    await page.getByRole('button', { name: 'Salvar dados de contrato' }).click();
    await expect(page.getByText('Salvo.')).toBeVisible();

    await page.goto('/admin/mentorados/lista');
    const linha = main.locator('text=Carlos Menezes').locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    await expect(page.getByText('Diagnóstico Inicial', { exact: true })).toBeVisible();

    await page.getByLabel('Faturamento anual (R$)').fill('600000');
    await page.getByLabel('Nº de colaboradores').fill('6');
    await page.getByLabel('Empresa regularizada?').selectOption({ label: 'Sim' });
    await page.getByLabel('Cultura construída?').selectOption({ label: 'Em construção' });
    await page.getByRole('button', { name: 'Salvar Diagnóstico Inicial' }).click();
    await expect(page.getByText('Salvo.')).toBeVisible();

    // E17/M27 (change request pós-MVP, 19/07/2026) — mesma tela, mesmo ator (Gestão de
    // Performance), mesmo Modulo.MENTORADOS — ver ROADMAP.md § "Blueprint (M27)".
    // onSalvo (recarrega o mentorado em segundo plano, GET /admin/mentorados/{id}) não é aguardado
    // pelo handleSubmit — espera o próprio refetch terminar antes de seguir, senão o reload logo
    // abaixo pode correr antes do PATCH ter sido confirmado pelo backend.
    const recarregouAposFerramentas = page.waitForResponse(
      (res) => res.url().includes('/admin/mentorados') && res.request().method() === 'GET' && res.ok(),
    );
    // M28 — "Ferramentas obrigatórias" também é label do card de métricas no topo da página
    // (MetricasCard), então o texto aparece 2x agora; .first() só prova presença da seção.
    await expect(page.getByText('Ferramentas obrigatórias', { exact: true }).first()).toBeVisible();
    await page.getByLabel('DRE estruturada').selectOption({ label: 'Sim' });
    await page.getByLabel('Manual de cultura').selectOption({ label: 'Em construção' });
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/ferramentas-obrigatorias') && res.ok()),
      page.getByRole('button', { name: 'Salvar ferramentas obrigatórias' }).click(),
    ]);
    await recarregouAposFerramentas;

    const recarregouAposAcompanhamento = page.waitForResponse(
      (res) => res.url().includes('/admin/mentorados') && res.request().method() === 'GET' && res.ok(),
    );
    await expect(page.getByText('Acompanhamento', { exact: true })).toBeVisible();
    await page.getByLabel('Nível de engajamento').selectOption({ label: 'Alto' });
    await page.getByLabel('Risco de churn').selectOption({ label: 'Atenção' });
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/acompanhamento') && res.ok()),
      page.getByRole('button', { name: 'Salvar acompanhamento' }).click(),
    ]);
    await recarregouAposAcompanhamento;

    // Recarrega a página (GET /admin/mentorados/{id}) pra confirmar que persistiu de verdade, não
    // só o estado local dos formulários.
    await page.reload();
    await expect(page.getByLabel('Faturamento anual (R$)')).toHaveValue('600000');
    await expect(page.getByLabel('Cultura construída?')).toHaveValue('EM_CONSTRUCAO');
    await expect(page.getByLabel('DRE estruturada')).toHaveValue('SIM');
    await expect(page.getByLabel('Manual de cultura')).toHaveValue('EM_CONSTRUCAO');
    await expect(page.getByLabel('Nível de engajamento')).toHaveValue('ALTO');
    await expect(page.getByLabel('Risco de churn')).toHaveValue('ATENCAO');
  });
});
