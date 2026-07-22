import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M10 — E4 Tarefas & Agenda', () => {
  test('mentorado cria uma tarefa, inicia, conclui, reabre, filtra e busca', async ({ page }) => {
    // Título único por execução — mesma razão do M09 (Meta/Tarefa não têm endpoint de exclusão).
    const titulo = `Renegociar fornecedor X ${Date.now()}`;

    await loginAs(page, 'marina@sabordamarina.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Encaminhamentos' }).click();
    await expect(page).toHaveURL(/\/mentorado\/tarefas/);

    // Criar
    await page.getByRole('button', { name: 'Novo encaminhamento' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Prazo').fill('2026-12-31');
    await page.getByLabel('Prioridade').selectOption('ALTA');
    await page.getByRole('button', { name: 'Criar encaminhamento' }).click();

    const linha = () => page.locator('[data-testid^="tarefa-row-"]', { hasText: titulo });
    await expect(linha()).toBeVisible();
    await expect(linha().getByText('Pendente')).toBeVisible();
    await expect(linha().getByText('Alta')).toBeVisible();

    // Filtrar por Pendentes -> aparece
    await page.getByRole('button', { name: 'Pendentes' }).click();
    await expect(linha()).toBeVisible();

    // Buscar por um trecho do título único -> aparece; busca por algo que não bate -> some
    await page.getByRole('button', { name: 'Todas' }).click();
    await page.getByPlaceholder('Buscar encaminhamentos...').fill(titulo);
    await expect(linha()).toBeVisible();
    await page.getByPlaceholder('Buscar encaminhamentos...').fill('xxxxxxxxxxxxxxxxxxxxnaoexiste');
    await expect(page.getByText('Nenhum encaminhamento encontrado.')).toBeVisible();
    await page.getByPlaceholder('Buscar encaminhamentos...').fill('');

    // Iniciar -> Em andamento
    await linha().getByRole('button', { name: 'Iniciar' }).click();
    await expect(linha().getByText('Em andamento')).toBeVisible();

    // Concluir -> o filtro atual é "Todas" (nunca saiu dele desde a busca acima), então a linha
    // CONTINUA visível — só o pill de status muda. Essa espera (em vez de checar count(0), que só
    // "passaria" por coincidência durante o instante em que tarefas===null/Carregando) é o que
    // sincroniza com o recarregamento disparado por avancarStatus() antes do próximo clique.
    await linha().getByRole('button', { name: 'Concluir' }).click();
    await expect(linha().getByText('Concluída')).toBeVisible();
    await page.getByRole('button', { name: 'Concluídas' }).click();
    await expect(linha()).toBeVisible();
    await expect(linha().getByRole('button', { name: 'Editar' })).toHaveCount(0);

    // Reabrir -> volta pra Pendente
    await linha().getByRole('button', { name: 'Reabrir' }).click();
    await expect(page.getByText(titulo)).toHaveCount(0);
    await page.getByRole('button', { name: 'Todas' }).click();
    await expect(linha().getByText('Pendente')).toBeVisible();
  });

  test('resumo reflete o dado real seedado de Fernanda Lima (10 tarefas, todas concluídas)', async ({ page }) => {
    // DemoDataSeeder: Fernanda tem 10 encaminhamentos, todos concluído=true no seed original.
    await loginAs(page, 'fernanda@cantinadafernanda.com.br');
    await page.getByRole('link', { name: 'Encaminhamentos' }).click();
    await expect(page.getByText('10 encaminhamento(s) no total.')).toBeVisible();
  });

  test('isolamento por tenant: mentorado só vê as próprias tarefas seedadas', async ({ page }) => {
    // Carlos Menezes: 10 encaminhamentos do seedMentorados. A ata da mentoria em grupo com Ana
    // (ver DemoDataSeeder) fica deliberadamente em RASCUNHO — nunca publicada em nenhum spec
    // (ver mentorias.spec.ts "ata ainda não publicada nunca aparece...") — então suas sugestões
    // nunca viram Encaminhamento real; não soma ao total. Se a query vazasse dado de outro
    // mentorado, o total não bateria com o seed conhecido.
    await loginAs(page, 'carlos@pointdocarlos.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const res = await page.request.get('/api/v1/mentorado/tarefas');
    expect(res.status()).toBe(200);
    const tarefas = await res.json();
    expect(tarefas).toHaveLength(10);
  });
});

// H4.6 (Fase 5, 22/07/2026, pedido do Marcos) — a tela Admin de Tarefas só tinha listagem + CSV,
// sem forma de editar título/prazo/prioridade nem avançar status de um encaminhamento existente
// (inclusive os materializados na publicação de ata, que nunca têm prazo de origem).
test.describe('H4.6 — Admin edita e avança status de um encaminhamento existente', () => {
  // Um só login pro describe inteiro (não um por teste) — o rate-limit de tentativas de login
  // (achado ao vivo nesta sessão) dispara com poucos logins seguidos no mesmo arquivo de spec.
  test('Gestão de Performance edita título/prazo/prioridade, avança status, e edição sem prazo não força uma data', async ({ page }) => {
    await loginAs(page, 'gestao_perf@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);

    await page.goto('/admin/mentorados/tarefas');
    const linha = page.locator('[data-testid^="tarefa-admin-row-"]', { hasText: 'Rafael Gomes' }).first();
    await expect(linha).toBeVisible();
    const tarefaId = (await linha.getAttribute('data-testid'))!.replace('tarefa-admin-row-', '');

    const novoTitulo = `Encaminhamento editado ${Date.now()}`;
    await linha.getByRole('button', { name: 'Editar' }).click();

    const linhaEditando = page.getByTestId(`tarefa-admin-editando-${tarefaId}`);
    await page.getByTestId(`tarefa-admin-titulo-${tarefaId}`).fill(novoTitulo);
    await linhaEditando.locator('input[type="date"]').fill('2026-09-15');
    await linhaEditando.locator('select').selectOption('ALTA');
    await linhaEditando.getByRole('button', { name: 'Salvar' }).click();

    const linhaAtualizada = page.getByTestId(`tarefa-admin-row-${tarefaId}`);
    await expect(linhaAtualizada.getByText(novoTitulo)).toBeVisible();
    await expect(linhaAtualizada.getByText('15/09/2026')).toBeVisible();
    await expect(linhaAtualizada.getByText('Alta')).toBeVisible();

    // Concluir -> some o botão Editar (mesmo padrão do lado mentorado).
    await linhaAtualizada.getByRole('button', { name: 'Concluir' }).click();
    await expect(linhaAtualizada.getByText('Concluída')).toBeVisible();
    await expect(linhaAtualizada.getByRole('button', { name: 'Editar' })).toHaveCount(0);

    // Reabrir -> volta pra Pendente, Editar reaparece.
    await linhaAtualizada.getByRole('button', { name: 'Reabrir' }).click();
    await expect(linhaAtualizada.getByText('Pendente')).toBeVisible();
    await expect(linhaAtualizada.getByRole('button', { name: 'Editar' })).toBeVisible();

    // Marina Souza (M06/DemoDataSeeder): encaminhamentos vêm de ata publicada, sem prazo de
    // origem — editar (mesmo só o título) não deve forçar a inventar uma data.
    const linhaSemPrazo = page.locator('[data-testid^="tarefa-admin-row-"]', { hasText: 'Marina Souza' })
      .filter({ hasText: 'Sem prazo' }).first();
    await expect(linhaSemPrazo).toBeVisible();
    const idSemPrazo = (await linhaSemPrazo.getAttribute('data-testid'))!.replace('tarefa-admin-row-', '');

    await linhaSemPrazo.getByRole('button', { name: 'Editar' }).click();
    await page.getByTestId(`tarefa-admin-editando-${idSemPrazo}`).getByRole('button', { name: 'Salvar' }).click();

    await expect(page.getByTestId(`tarefa-admin-row-${idSemPrazo}`).getByText('Sem prazo')).toBeVisible();
  });
});
