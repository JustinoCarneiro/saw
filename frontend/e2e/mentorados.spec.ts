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
    await page.getByLabel('Plano fechado').selectOption({ label: 'Básico' });
    await page.getByRole('button', { name: 'Confirmar' }).click();
    await expect(linhaLead.getByText('Fechado', { exact: true })).toBeVisible();

    // 3) Fundador cria a conta de mentorado a partir do lead fechado (fecha a pendência do M05).
    // LoginPage redireciona pra /admin se já houver sessão ativa (Paula) — sem limpar os cookies
    // primeiro, o formulário de login nunca aparece e o teste trava esperando o campo de e-mail.
    await page.context().clearCookies();
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/mentorados/lista');
    await main.getByRole('button', { name: 'Criar a partir de um lead' }).click();
    await page.getByLabel('Lead').selectOption({ label: `${nome} — ${email}` });
    await page.getByRole('button', { name: 'Criar mentorado' }).click();
    await expect(page.getByText(`Mentorado criado: ${nome}`)).toBeVisible();
    await expect(page.getByText('Senha temporária:')).toBeVisible();
    await page.getByRole('button', { name: 'Entendi' }).click();

    // 4) Cria a mentoria com esse mentorado recém-criado.
    await page.goto('/admin/mentorados/mentorias');
    await main.getByRole('button', { name: 'Nova mentoria' }).click();
    await page.getByLabel('Mentor').selectOption({ label: 'Lucas Alves' });
    await page.getByLabel(nome).check();
    await page.getByLabel('Data e hora').fill('2026-08-15T14:00');
    await page.getByLabel('Duração (min)').fill('45');
    await main.getByRole('button', { name: 'Criar mentoria' }).click();

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

  test('Conteúdos: criar e publicar', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
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

    await linha.getByRole('button', { name: 'Publicar' }).click();
    await expect(linha.getByText('Publicado')).toBeVisible();
  });

  test('Eventos: criar e avançar até Realizado', async ({ page }) => {
    await loginAs(page, 'matheus@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await page.goto('/admin/conteudos/eventos');

    const titulo = `Evento E2E ${Date.now()}`;
    const main = page.getByRole('main');
    await main.getByRole('button', { name: 'Novo evento' }).click();
    await page.getByLabel('Título').fill(titulo);
    await page.getByLabel('Data e hora').fill('2026-10-01T19:00');
    await main.getByRole('button', { name: 'Salvar' }).click();

    const linha = main.locator('text=' + titulo).locator('xpath=ancestor::div[contains(@class,"row")]');
    await expect(linha.getByText('Programado')).toBeVisible();

    await linha.getByRole('button', { name: 'Iniciar' }).click();
    await expect(linha.getByText('Ao vivo', { exact: true })).toBeVisible();

    await linha.getByRole('button', { name: 'Finalizar' }).click();
    await expect(linha.getByText('Realizado')).toBeVisible();
  });

  test('RBAC: área sem Mentorados/Conteúdos não vê os módulos nem acessa via URL direta', async ({ page }) => {
    await loginAs(page, 'paula@sawhub.com.br');
    await expect(page).toHaveURL(/\/admin\//);
    await expect(page.getByRole('link', { name: 'Mentorados' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Conteúdos' })).toHaveCount(0);

    await page.goto('/admin/mentorados/lista');
    await expect(page.getByText('Sem acesso')).toBeVisible();

    await page.goto('/admin/conteudos/lista');
    await expect(page.getByText('Sem acesso')).toBeVisible();
  });
});
