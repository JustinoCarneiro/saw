import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M06 — Mentorados, Mentorias, Ata e diferencial de IA', () => {
  test('Fluxo completo: lead público -> fechado -> mentorado -> mentoria -> ata publicada', async ({ page }) => {
    const timestamp = Date.now();
    const nome = `Lead M06 E2E ${timestamp}`;
    const email = `m06.${timestamp}@example.com`;

    // 1) Lead nasce pela jornada pública (H1.3), igual ao comercial.spec.ts.
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    // 2) Paula (Comercial) leva o lead até Fechado.
    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');
    const main = page.getByRole('main');
    const linhaLead = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linhaLead.getByRole('button', { name: 'Mover p/ Em contato' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await linhaLead.getByRole('button', { name: 'Avançar p/ Proposta' }).click();
    await page.getByRole('button', { name: 'Confirmar' }).click();
    // M25 — "Fechar venda" abre o formulário único de venda (POST .../fechar-venda), substituindo
    // o antigo "Plano fechado" na UI.
    await linhaLead.getByRole('button', { name: 'Fechar venda' }).click();
    await page.getByLabel('Produto vendido').selectOption({ label: 'Mentoria contínua' });
    await page.getByLabel('Origem da venda').selectOption({ label: 'Direta' });
    await page.getByLabel('Valor total da venda').fill('26000');
    await page.getByLabel('Valor pago no ato').fill('6000');
    await page.getByLabel('Forma de pagamento').selectOption({ label: 'Pix' });
    await page.getByRole('button', { name: 'Confirmar venda' }).click();
    await expect(linhaLead.getByText('Fechado', { exact: true })).toBeVisible();

    // 3) Fundador cria a conta de mentorado a partir do lead fechado (fecha a pendência do M05).
    // LoginPage redireciona pra /admin se já houver sessão ativa (Paula) — sem limpar os cookies
    // primeiro, o formulário de login nunca aparece e o teste trava esperando o campo de e-mail.
    await page.context().clearCookies();
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');
    await main.getByRole('button', { name: 'Criar a partir de um lead' }).click();
    await page.getByLabel('Lead').selectOption({ label: `${nome} — ${email}` });
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await expect(page.getByText('Senha temporária:')).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    // M25 (Suposição 6) — produtoVenda/valorTotalVenda/dataFechamento do Lead propagam pro
    // Mentorado (tipoContrato/valorContrato/dataFechamentoContrato) sem precisar redigitar.
    // M28 ("página dedicada de mentorado") — "Editar" virou "Ver perfil" e navega pra uma página
    // própria (/admin/mentorados/lista/:id) em vez de expandir um form inline nesta tela.
    const linhaMentoradoCriado = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linhaMentoradoCriado.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);
    await expect(page.getByText('Dados de contrato', { exact: true })).toBeVisible();
    await expect(page.getByLabel('Tipo de contrato')).toHaveValue('MENTORIA_CONTINUA');
    await expect(page.getByLabel('Valor do contrato (R$)')).toHaveValue('26000');

    // 4) Cria a mentoria com esse mentorado recém-criado.
    await page.goto('/admin/mentorados/mentorias');
    await main.getByRole('button', { name: 'Nova mentoria' }).click();
    await page.getByLabel('Mentor').selectOption({ label: 'Gestão de Performance' });
    await page.getByLabel(nome).check();
    // Auditoria de UX (22/07/2026) — DataHoraInput trocou o datetime-local nativo (formato AM/PM
    // dependente do navegador) por date + dois <select> de hora/minuto, sempre 24h. "Hora"
    // precisa de exact:true — sem isso colide com o texto "Data e hora" do próprio label.
    await page.getByLabel('Data e hora').fill('2026-08-15');
    await page.getByLabel('Hora', { exact: true }).selectOption('14');
    await page.getByLabel('Minuto', { exact: true }).selectOption('00');
    await page.getByLabel('Duração (min)').fill('45');
    // M28 ("reorganizar lista de mentorias") — a lista central passou a mostrar só Grupo por
    // padrão; esta é uma mentoria Individual (tipo default do form), então precisa do filtro pra
    // aparecer aqui (histórico individual "de verdade" agora vive na página do mentorado). Espera
    // o POST de criação E o GET que ele dispara em seguida (onCriada→carregar(), ainda com
    // tipo=GRUPO no closure daquele momento) terminarem ANTES de trocar o filtro — só esperar o
    // form fechar (DOM) não basta, o GET disparado pelo onCriada só é AGENDADO nesse instante, não
    // concluído (ver comentário em MentoriasAgendaPage.tsx e achado ao vivo neste E2E). Listeners
    // registrados ANTES do clique (senão, se a resposta já tiver chegado antes do registro, a
    // promise fica esperando pra sempre).
    const postCriacao = page.waitForResponse((res) => res.url().includes('/admin/mentorias') && res.request().method() === 'POST' && res.ok());
    const getAposCriacao = page.waitForResponse((res) => res.url().includes('/admin/mentorias?') && res.request().method() === 'GET' && res.ok());
    await main.getByRole('button', { name: 'Criar mentoria' }).click();
    await postCriacao;
    await getAposCriacao;

    const getAposFiltro = page.waitForResponse((res) => res.url().includes('tipo=INDIVIDUAL') && res.request().method() === 'GET' && res.ok());
    await page.getByLabel('Filtro de tipo (agenda)').selectOption({ label: 'Individual' });
    await getAposFiltro;
    const linhaMentoria = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linhaMentoria.getByText('Agendada', { exact: true })).toBeVisible();

    // 5) Confirmar -> Realizar (cria a ata e navega pra ela).
    await linhaMentoria.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linhaMentoria.getByText('Confirmada', { exact: true })).toBeVisible();
    await linhaMentoria.getByRole('button', { name: 'Realizar' }).click();
    await expect(page).toHaveURL(/\/ata$/);

    // 6) Sem áudio/API key de IA nesta verificação — escreve o resumo manualmente e publica.
    await expect(page.getByText('Sem áudio')).toBeVisible();
    await page.getByPlaceholder(/Escreva o resumo da mentoria/).fill('Resumo escrito manualmente no teste E2E.');
    await page.getByRole('button', { name: 'Salvar resumo' }).click();
    // exact: true — "Suba a gravação..." (texto de ajuda do upload) contém a palavra "rascunho"
    // no meio da frase, então uma busca por substring bate ali também.
    await expect(page.getByText('Rascunho', { exact: true })).toBeVisible();

    // M23 — "Publicar ata" agora abre um ConfirmDialog antes de publicar de verdade.
    await page.getByRole('button', { name: 'Publicar ata' }).click();
    await page.getByRole('button', { name: 'Sim, publicar' }).click();
    await expect(page.getByText('Publicada', { exact: true })).toBeVisible();
    // Depois de publicada, o formulário de upload/edição some (ver AtaDetalhePage `!publicada`).
    await expect(page.getByRole('button', { name: 'Publicar ata' })).toHaveCount(0);
  });

  // O teste acima escreve o resumo manualmente porque, até aqui, ninguém tinha como testar o
  // pipeline de IA de ponta a ponta sem gastar chamada real na Whisper/Claude — diferente de
  // Google OAuth e Mercado Pago (que ao menos têm o caminho "não configurado" coberto), este
  // endpoint (POST .../ata/audio) não tinha NENHUM teste E2E. Fecha isso com um stub HTTP local
  // (scripts/e2e-ia-stub-server.mjs, ver e2e-up.sh) que devolve texto/rascunho fixos — não prova
  // que a Whisper/Claude reais respondem nesse formato (isso é responsabilidade de quem mantém o
  // contrato de API deles), só prova que o pipeline do SAW HUB consome a resposta corretamente.
  test('upload de áudio aciona o pipeline de IA (Whisper + Claude via stub) e o rascunho vira ata publicada', async ({ page }) => {
    const timestamp = Date.now();
    const nome = `Lead IA E2E ${timestamp}`;
    const email = `ia.${timestamp}@example.com`;
    const main = page.getByRole('main');

    // 1-4) Mesmo caminho do teste "Fluxo completo" acima até ter uma mentoria CONFIRMADA própria
    // (evita depender/competir por uma ata seedada compartilhada com outros testes).
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');
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

    await page.context().clearCookies();
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');
    await main.getByRole('button', { name: 'Criar a partir de um lead' }).click();
    await page.getByLabel('Lead').selectOption({ label: `${nome} — ${email}` });
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    await page.goto('/admin/mentorados/mentorias');
    await main.getByRole('button', { name: 'Nova mentoria' }).click();
    await page.getByLabel('Mentor').selectOption({ label: 'Gestão de Performance' });
    await page.getByLabel(nome).check();
    await page.getByLabel('Data e hora').fill('2026-08-16');
    await page.getByLabel('Hora', { exact: true }).selectOption('10');
    await page.getByLabel('Minuto', { exact: true }).selectOption('00');
    await page.getByLabel('Duração (min)').fill('45');
    // M28 — mesma razão do teste anterior: mentoria Individual não aparece mais por padrão nesta
    // lista central (Grupo é o padrão desde a reorganização). Espera o POST de criação E o GET
    // que ele dispara em seguida (onCriada→carregar(), ainda com tipo=GRUPO no closure daquele
    // momento) terminarem antes de trocar o filtro — só esperar o form fechar (DOM) não basta.
    // Listeners registrados ANTES do clique (senão a promise pode ficar esperando pra sempre).
    const postCriacao = page.waitForResponse((res) => res.url().includes('/admin/mentorias') && res.request().method() === 'POST' && res.ok());
    const getAposCriacao = page.waitForResponse((res) => res.url().includes('/admin/mentorias?') && res.request().method() === 'GET' && res.ok());
    await main.getByRole('button', { name: 'Criar mentoria' }).click();
    await postCriacao;
    await getAposCriacao;

    const getAposFiltro = page.waitForResponse((res) => res.url().includes('tipo=INDIVIDUAL') && res.request().method() === 'GET' && res.ok());
    await page.getByLabel('Filtro de tipo (agenda)').selectOption({ label: 'Individual' });
    await getAposFiltro;
    const linhaMentoria = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linhaMentoria.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linhaMentoria.getByText('Confirmada', { exact: true })).toBeVisible();
    await linhaMentoria.getByRole('button', { name: 'Realizar' }).click();
    await expect(page).toHaveURL(/\/ata$/);

    // 5) Sobe um "áudio" (conteúdo não importa — o stub nem valida, mesmo raciocínio de qualquer
    // fake de integração externa; extensão/content-type precisam passar pela validação real do
    // AudioStorageService, essa parte não é stubada).
    await page.locator('input[type="file"]').setInputFiles({
      name: 'mentoria.mp3',
      mimeType: 'audio/mpeg',
      buffer: Buffer.from('conteudo-fake-e2e'),
    });
    await page.getByRole('button', { name: 'Enviar áudio' }).click();

    // 6) Pipeline assíncrono (AtaProcessamentoService, thread separada da request) — a própria
    // tela já faz polling a cada 3s (ver AtaDetalhePage). Não afirma o estado "Processando…" no
    // meio do caminho: contra um stub local (rápido), o processamento pode concluir antes do
    // próximo poll do teste conseguir flagrar esse estado transiente — só o resultado final
    // importa aqui.
    await expect(page.getByText('IA concluída')).toBeVisible({ timeout: 15_000 });

    // 7) Transcrição e resumo (pré-preenchido pelo rascunho) batem com o stub. Auditoria de UX
    // (22/07/2026) — encaminhamentos não são mais sugeridos pela IA (processo real da SAW no
    // Notion é digitar direto); o mentor adiciona manualmente aqui.
    await expect(page.getByText(/Transcrição de teste E2E/)).toBeVisible();
    await expect(page.getByPlaceholder(/Escreva o resumo da mentoria/)).toHaveValue(/Resumo gerado pela IA \(stub E2E\)/);
    await page.getByPlaceholder('Novo encaminhamento…').fill('Atualizar ficha técnica com os novos preços');
    await page.getByRole('button', { name: 'Adicionar' }).click();
    // Título da sugestão é um <input> (SugestaoRow) — toHaveValue espera/re-tenta até o POST +
    // re-render (onCriado→onSalvo→carregar) terminarem, ao contrário de evaluateAll (one-shot).
    await expect(page.locator('[data-testid^="sugestao-titulo-"]')).toHaveValue('Atualizar ficha técnica com os novos preços');

    // 8) Publica com o encaminhamento digitado manualmente — fecha o pipeline completo, do
    // upload até virar ata publicada de verdade.
    await page.getByRole('button', { name: 'Publicar ata' }).click();
    await page.getByRole('button', { name: 'Sim, publicar' }).click();
    await expect(page.getByText('Publicada', { exact: true })).toBeVisible();
  });

  // M28 (change request, 21/07/2026) — "colar transcrição do Google Meet", alternativa aditiva ao
  // upload de áudio testado acima. Mesmo pipeline de IA (Claude via stub) a partir daqui, só pula
  // o passo de transcrição (Whisper) — o texto colado já É a transcrição.
  test('colar transcrição do Meet aciona o mesmo pipeline de IA, sem passar pelo Whisper', async ({ page }) => {
    const timestamp = Date.now();
    const nome = `Lead Transcricao E2E ${timestamp}`;
    const email = `transcricao.${timestamp}@example.com`;
    const main = page.getByRole('main');

    // 1-4) Mesmo caminho dos testes acima até ter uma mentoria CONFIRMADA própria.
    await page.goto('/solicitar-acesso');
    await page.getByLabel('Nome').fill(nome);
    await page.getByLabel('E-mail').fill(email);
    await page.getByRole('button', { name: 'Enviar solicitação' }).click();
    await expect(page.getByText('Solicitação enviada.')).toBeVisible();

    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/comercial/leads');
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

    await page.context().clearCookies();
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');
    await main.getByRole('button', { name: 'Criar a partir de um lead' }).click();
    await page.getByLabel('Lead').selectOption({ label: `${nome} — ${email}` });
    await page.getByRole('button', { name: 'Criar mentorado', exact: true }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    await page.goto('/admin/mentorados/mentorias');
    await main.getByRole('button', { name: 'Nova mentoria' }).click();
    await page.getByLabel('Mentor').selectOption({ label: 'Gestão de Performance' });
    await page.getByLabel(nome).check();
    await page.getByLabel('Data e hora').fill('2026-08-17');
    await page.getByLabel('Hora', { exact: true }).selectOption('10');
    await page.getByLabel('Minuto', { exact: true }).selectOption('00');
    await page.getByLabel('Duração (min)').fill('45');
    const postCriacao = page.waitForResponse((res) => res.url().includes('/admin/mentorias') && res.request().method() === 'POST' && res.ok());
    const getAposCriacao = page.waitForResponse((res) => res.url().includes('/admin/mentorias?') && res.request().method() === 'GET' && res.ok());
    await main.getByRole('button', { name: 'Criar mentoria' }).click();
    await postCriacao;
    await getAposCriacao;

    const getAposFiltro = page.waitForResponse((res) => res.url().includes('tipo=INDIVIDUAL') && res.request().method() === 'GET' && res.ok());
    await page.getByLabel('Filtro de tipo (agenda)').selectOption({ label: 'Individual' });
    await getAposFiltro;
    const linhaMentoria = main.locator('text=' + nome).locator('xpath=ancestor::div[contains(@class,"row")]');
    await linhaMentoria.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linhaMentoria.getByText('Confirmada', { exact: true })).toBeVisible();
    await linhaMentoria.getByRole('button', { name: 'Realizar' }).click();
    await expect(page).toHaveURL(/\/ata$/);

    // 5) Cola a transcrição em vez de subir áudio — troca de aba primeiro.
    await expect(page.getByRole('tab', { name: 'Subir áudio' })).toHaveAttribute('aria-selected', 'true');
    await page.getByRole('tab', { name: 'Colar transcrição' }).click();
    await expect(page.getByRole('tab', { name: 'Colar transcrição' })).toHaveAttribute('aria-selected', 'true');
    await page.getByPlaceholder(/Cole aqui a transcrição/).fill('Transcrição colada manualmente do Google Meet, teste E2E.');
    await page.getByRole('button', { name: 'Processar transcrição' }).click();

    // 6) Mesmo pipeline assíncrono/polling de status do teste de áudio acima.
    await expect(page.getByText('IA concluída')).toBeVisible({ timeout: 15_000 });

    // 7) A transcrição exibida é literalmente o texto colado (não passou pelo Whisper) — resumo e
    // sugestões vêm do mesmo stub de Claude usado no upload de áudio.
    await expect(page.getByText('Transcrição colada manualmente do Google Meet, teste E2E.')).toBeVisible();
    await expect(page.getByPlaceholder(/Escreva o resumo da mentoria/)).toHaveValue(/Resumo gerado pela IA \(stub E2E\)/);

    await page.getByRole('button', { name: 'Publicar ata' }).click();
    await page.getByRole('button', { name: 'Sim, publicar' }).click();
    await expect(page.getByText('Publicada', { exact: true })).toBeVisible();
  });

  // Fase 5 (H11.1) — antes desta feature, telefone/bio/foto só podiam ser preenchidos pelo
  // próprio mentorado logando (H9.1); o Admin não tinha como completar o cadastro (achado
  // relatado pelo cliente: só nome/e-mail/plano apareciam pra editar). areasInteresse removido
  // no M23 (change request pós-MVP, confirmado pelo cliente como não aplicável).
  test('Admin preenche telefone/bio/foto de um mentorado sem depender dele logar', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');

    // Carlos Menezes nasce no seed sem telefone/bio/áreas (ver DemoDataSeeder) — bom fixture pro
    // caso real reportado: Admin prospectando/completando um cadastro ainda vazio.
    const main = page.getByRole('main');
    const linha = main.locator('text=Carlos Menezes').locator('xpath=ancestor::div[contains(@class,"row")]');
    await linha.getByRole('button', { name: 'Ver perfil' }).click();
    await expect(page).toHaveURL(/\/admin\/mentorados\/lista\/.+/);

    const foto = `https://exemplo.com/foto-e2e-${Date.now()}.jpg`;
    await page.getByLabel('Telefone').fill('(11) 90000-1234');
    await page.getByLabel('Foto (URL)').fill(foto);
    await page.getByLabel('Bio').fill('Bio preenchida pelo Admin no teste E2E.');
    await page.getByRole('button', { name: 'Salvar', exact: true }).click();
    await expect(page.getByText('Salvo.')).toBeVisible();

    // Recarrega a página (GET /admin/mentorados/{id}) pra provar que persistiu de verdade, não só
    // que o estado local do formulário manteve o valor digitado.
    await page.reload();
    await expect(page.getByLabel('Telefone')).toHaveValue('(11) 90000-1234');
    await expect(page.getByLabel('Foto (URL)')).toHaveValue(foto);
    await expect(page.getByLabel('Bio')).toHaveValue('Bio preenchida pelo Admin no teste E2E.');
  });

  test('Admin cura materiais recomendados numa ata ainda em rascunho', async ({ page }) => {
    // Ana/Carlos, mentor Ricardo Costa: mentoria em grupo REALIZADA cuja ata fica
    // deliberadamente em RASCUNHO no seed (ver DemoDataSeeder + mentorias.spec.ts) — bom fixture
    // porque continua editável (ao contrário da ata de João, publicada no seed).
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/mentorias');

    const linha = page.locator('[data-testid^="mentoria-row-"]', { hasText: 'Ricardo Costa' });
    await linha.getByRole('button', { name: 'Ver ata' }).click();
    await expect(page).toHaveURL(/\/ata$/);

    const card = page.getByTestId('materiais-card');
    await expect(card.getByText('Materiais recomendados')).toBeVisible();
    await card.getByPlaceholder('Buscar conteúdo...').fill('DRE');
    const item = card.getByText('Como calcular seu DRE');
    await expect(item).toBeVisible();
    // Idempotente: se uma execução anterior (mesmo banco de E2E persistido entre runs) já deixou
    // marcado, não desmarca nem tenta salvar de novo (o botão fica disabled sem alteração real,
    // então clicar travaria esperando uma resposta que nunca vem) — só confere que persiste.
    const checkbox = card.getByRole('checkbox');
    if (!(await checkbox.isChecked())) {
      await item.click();
      await Promise.all([
        page.waitForResponse((res) => res.url().includes('/materiais') && res.ok()),
        card.getByRole('button', { name: 'Salvar materiais' }).click(),
      ]);
    }

    await page.reload();
    await page.getByTestId('materiais-card').getByPlaceholder('Buscar conteúdo...').fill('DRE');
    await expect(page.getByTestId('materiais-card').getByRole('checkbox')).toBeChecked();
  });

  // E17/M27 (change request pós-MVP, 19/07/2026) — mesmo fixture GRUPO/REALIZADA de Ana+Carlos
  // (mentor Ricardo Costa) do teste de materiais acima; presença é aditiva, não afeta ata/materiais.
  test('Admin marca presença numa mentoria em grupo, e o valor persiste após reload', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/mentorias');

    const linha = page.locator('[data-testid^="mentoria-row-"]', { hasText: 'Ricardo Costa' });
    await linha.getByRole('button', { name: 'Ver ata' }).click();
    await expect(page).toHaveURL(/\/ata$/);

    const card = page.getByTestId('presenca-card');
    await expect(card.getByText('Presença', { exact: true })).toBeVisible();
    const itemAna = card.locator('label', { hasText: 'Ana Costa' }).getByRole('checkbox');

    const estadoInicial = await itemAna.isChecked();
    await itemAna.setChecked(!estadoInicial);
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/presencas') && res.ok()),
      card.getByRole('button', { name: 'Salvar presença' }).click(),
    ]);

    await page.reload();
    await expect(page.getByTestId('presenca-card').locator('label', { hasText: 'Ana Costa' }).getByRole('checkbox'))
      .toBeChecked({ checked: !estadoInicial });

    // Restaura o estado original pra não deixar side-effect entre execuções do E2E.
    const itemAnaDepois = page.getByTestId('presenca-card').locator('label', { hasText: 'Ana Costa' }).getByRole('checkbox');
    await itemAnaDepois.setChecked(estadoInicial);
    await Promise.all([
      page.waitForResponse((res) => res.url().includes('/presencas') && res.ok()),
      page.getByTestId('presenca-card').getByRole('button', { name: 'Salvar presença' }).click(),
    ]);
  });

  test('Conteúdos: criar e publicar', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/lista');

    const titulo = `Conteúdo E2E ${Date.now()}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Novo conteúdo' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('URL').fill('https://cdn.sawhub.com.br/e2e-teste.pdf');
    await page.getByRole('button', { name: 'Salvar' }).click();

    const linha = main.locator('text=' + titulo).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Rascunho')).toBeVisible();

    // Auditoria de UX (22/07/2026) — achado: PUT /admin/conteudos/{id} já existia no backend, mas
    // a tela nunca chamava (só criar/publicar/despublicar). "Editar" fecha essa lacuna.
    const tituloEditado = `${titulo} (editado)`;
    await linha.getByRole('button', { name: 'Editar' }).click();
    await expect(page.getByText('Editar conteúdo')).toBeVisible();
    await expect(page.getByLabel('Título')).toHaveValue(titulo);
    await page.getByLabel('Título').fill(tituloEditado);
    await page.getByRole('button', { name: 'Salvar' }).click();
    const linhaEditada = main.locator('text=' + tituloEditado).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linhaEditada.getByText('Rascunho')).toBeVisible();

    await linhaEditada.getByRole('button', { name: 'Publicar' }).click();
    await expect(linhaEditada.getByText('Publicado')).toBeVisible();
  });

  test('Eventos: criar e avançar até Realizado', async ({ page }) => {
    await loginAs(page, 'admin@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/eventos');

    const titulo = `Evento E2E ${Date.now()}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Novo evento' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Data e hora').fill('2026-10-01');
    await page.getByLabel('Hora', { exact: true }).selectOption('19');
    await page.getByLabel('Minuto', { exact: true }).selectOption('00');
    await main.getByRole('button', { name: 'Salvar' }).click();

    const linha = main.locator('text=' + titulo).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Programado')).toBeVisible();

    // Auditoria de UX (22/07/2026) — mesmo achado do Conteúdos acima: PUT /admin/eventos/{id} já
    // existia, faltava o botão. tipo (Ao vivo/Presencial) fica desabilitado — imutável após criar.
    await linha.getByRole('button', { name: 'Editar' }).click();
    await expect(page.getByText('Editar evento')).toBeVisible();
    await expect(page.getByLabel('Tipo')).toBeDisabled();
    await page.getByLabel('Local presencial (opcional)').fill('Sede SAW, Recife');
    await main.getByRole('button', { name: 'Salvar' }).click();
    await expect(linha.getByText('Programado')).toBeVisible();

    await linha.getByRole('button', { name: 'Iniciar' }).click();
    await expect(linha.getByText('Ao vivo', { exact: true })).toBeVisible();

    await linha.getByRole('button', { name: 'Finalizar' }).click();
    await expect(linha.getByText('Realizado')).toBeVisible();
  });

  test('RBAC: área sem Mentorados/Conteúdos não vê os módulos nem acessa via URL direta', async ({ page }) => {
    await loginAs(page, 'comercial@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await expect(page.getByRole('link', { name: 'Gestão de Performance' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Conteúdos' })).toHaveCount(0);

    await page.goto('/admin/mentorados/lista');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.goto('/admin/conteudos/lista');
    await expect(page.getByText('Sem acesso')).toBeVisible();
  });
});
