import type { Page } from '@playwright/test';

export const SEEDED_PASSWORD = 'trocar-no-primeiro-login';

export async function loginAs(page: Page, email: string, password = SEEDED_PASSWORD) {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

/** apiClient (axios) anexa X-XSRF-TOKEN sozinho a partir do cookie (ver shared/lib/apiClient.ts)
 * — page.request.* não tem esse interceptor, então qualquer POST/PUT/PATCH/DELETE feito direto
 * (sem clicar num botão real da UI) precisa desse header manualmente ou leva 403 do CSRF. */
export async function csrfHeaders(page: Page): Promise<Record<string, string>> {
  const cookies = await page.context().cookies();
  const token = cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
  return token ? { 'X-XSRF-TOKEN': token } : {};
}
