import { expect, test } from '@playwright/test';
import { loginAs } from './helpers';

test.describe('M09 — E3 Metas Estratégicas', () => {
  test('mentorado cria uma meta, edita o progresso, pausa, reativa e conclui', async ({ page }) => {
    // Título único por execução — Meta não tem endpoint de exclusão (self-service só
    // cria/edita/transiciona, ver Blueprint M09), então reruns acumulam linhas na mesma conta;
    // um título fixo colidiria com sobras de execuções anteriores (mesma classe de flake já
    // documentada no LeadRepositoryTest do M05).
    const titulo = `Reduzir CMV em 5% ${Date.now()}`;

    await loginAs(page, 'marina@sabordamarina.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    await page.getByRole('link', { name: 'Metas' }).click();
    await expect(page).toHaveURL(/\/mentorado\/metas/);

    // Criar
    await page.getByRole('button', { name: 'Nova meta' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Descrição (opcional)').fill('Renegociar 3 fornecedores principais');
    await page.getByLabel('Prazo').fill('2026-12-31');
    await page.getByRole('button', { name: 'Criar meta' }).click();

    // Escopo pelo data-testid do DataGridRow (prefixo meta-row-) + título único — sem isso,
    // reruns acumulam linhas na mesma conta (Meta não tem endpoint de exclusão) e um seletor
    // genérico tipo getByRole('button', { name: 'Editar' }) quebra em modo estrito com N linhas.
    const linha = () => page.locator('[data-testid^="meta-row-"]', { hasText: titulo });

    await expect(linha()).toBeVisible();
    await expect(linha().getByText('No prazo')).toBeVisible();

    // Editar progresso
    await linha().getByRole('button', { name: 'Editar' }).click();
    await page.getByLabel('Progresso (%)').fill('60');
    await page.getByRole('button', { name: 'Salvar alterações' }).click();
    await expect(linha().getByText('60%')).toBeVisible();

    // Pausar -> some da aba Ativas, aparece só na aba Pausadas
    await linha().getByRole('button', { name: 'Pausar' }).click();
    await expect(page.getByText(titulo)).toHaveCount(0);
    await page.getByRole('button', { name: 'Pausadas' }).click();
    await expect(linha()).toBeVisible();
    await expect(linha().getByText('Pausada')).toBeVisible();

    // Reativar -> volta pra Ativas
    await linha().getByRole('button', { name: 'Reativar' }).click();
    await expect(page.getByText(titulo)).toHaveCount(0);
    await page.getByRole('button', { name: 'Ativas' }).click();
    await expect(linha()).toBeVisible();

    // Concluir -> progresso vira 100%, some da aba Ativas, aparece em Concluídas sem ações
    await linha().getByRole('button', { name: 'Concluir' }).click();
    await expect(page.getByText(titulo)).toHaveCount(0);
    await page.getByRole('button', { name: 'Concluídas' }).click();
    await expect(linha()).toBeVisible();
    await expect(linha().getByText('100%')).toBeVisible();
    await expect(linha().getByRole('button', { name: 'Editar' })).toHaveCount(0);
  });

  test('resumo geral vem zerado pra mentorado sem nenhuma meta, sem quebrar', async ({ page }) => {
    // Carlos não tem Meta nenhuma (DemoDataSeeder não semeia Meta, M09 é self-service) — resumo
    // precisa vir zerado de forma limpa, não 500/NPE em cima de lista vazia.
    await loginAs(page, 'carlos@pointdocarlos.com.br');
    await page.getByRole('link', { name: 'Metas' }).click();
    await expect(page.getByText('Você concluiu 0 meta(s).')).toBeVisible();
  });

  test('isolamento por tenant: mentorado não vê meta de outro mentorado na listagem', async ({ page }) => {
    // Marina criou uma meta no primeiro teste deste arquivo — Ana não pode vê-la na própria
    // listagem. O caminho de escrita (editar/concluir meta de outro mentorado) é coberto por
    // MetaServiceTest (unit, backend) e verificado ao vivo via curl — sem duplicar aqui com uma
    // chamada de API crua que precisaria replicar o handshake de CSRF manualmente.
    await loginAs(page, 'ana@anacosta.com.br');
    await expect(page).toHaveURL(/\/mentorado/);

    const res = await page.request.get('/api/v1/mentorado/metas');
    expect(res.status()).toBe(200);
    expect(await res.json()).toEqual([]);
  });
});

// H3.4 (Fase 5, 22/07/2026, pedido do Marcos) — mesmo gap achado e corrigido no H4.6
// (Encaminhamentos): a tela Admin de Metas só tinha listagem + CSV, sem forma de criar/editar
// uma meta direto nem avançar status. Diferente de Encaminhamento, Meta nunca nasce de um fluxo
// automático — a tela também precisa de criação, não só edição.
test.describe('H3.4 — Admin cria, edita e avança status de uma meta', () => {
  test('Gestão de Performance cria uma meta pra um mentorado, edita e avança status até Concluir', async ({ page }) => {
    await loginAs(page, 'lucas@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin/);

    await page.goto('/admin/mentorados/metas');
    const titulo = `Meta admin E2E ${Date.now()}`;

    await page.getByRole('button', { name: 'Nova meta' }).click();
    await page.getByLabel('Mentorado').selectOption({ label: 'Rafael Gomes' });
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Prazo').fill('2026-11-30');
    await page.getByRole('button', { name: 'Criar meta' }).click();

    const linha = () => page.locator('[data-testid^="meta-admin-row-"]', { hasText: titulo });
    await expect(linha()).toBeVisible();
    await expect(linha().getByText('Ativa')).toBeVisible();
    const metaId = (await linha().getAttribute('data-testid'))!.replace('meta-admin-row-', '');

    await linha().getByRole('button', { name: 'Editar' }).click();
    const linhaEditando = page.getByTestId(`meta-admin-editando-${metaId}`);
    await linhaEditando.locator('input[type="number"]').fill('40');
    await linhaEditando.getByRole('button', { name: 'Salvar' }).click();

    const linhaAtualizada = page.getByTestId(`meta-admin-row-${metaId}`);
    await expect(linhaAtualizada.getByText('40%')).toBeVisible();

    // Concluir -> some o botão Editar (mesmo padrão do lado mentorado e dos Encaminhamentos).
    await linhaAtualizada.getByRole('button', { name: 'Concluir' }).click();
    await expect(linhaAtualizada.getByText('Concluída')).toBeVisible();
    await expect(linhaAtualizada.getByRole('button', { name: 'Editar' })).toHaveCount(0);
  });
});
