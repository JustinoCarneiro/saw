import type { Modulo } from './types';

// Mesma ordem de prioridade da sidebar — usada pra decidir pra onde mandar o usuário
// logo após o login (primeiro módulo que a área dele realmente permite).
export const MODULO_ROUTE_ORDER: { modulo: Modulo; path: string }[] = [
  { modulo: 'DASHBOARD', path: '/admin/dashboard' },
  { modulo: 'COMERCIAL', path: '/admin/comercial' },
  { modulo: 'FINANCEIRO', path: '/admin/financeiro' },
  { modulo: 'MENTORADOS', path: '/admin/mentorados' },
  // PAINEL_CONSOLIDADO não tem rota própria — virou a 1ª aba dentro de /admin/mentorados (todo
  // Area com PAINEL_CONSOLIDADO também tem MENTORADOS, ver AreaModuloMatrix no backend), então a
  // entrada MENTORADOS acima já cobre o redirecionamento pra quem só tinha PAINEL_CONSOLIDADO.
  { modulo: 'TIME', path: '/admin/time' },
  { modulo: 'CONTEUDOS', path: '/admin/conteudos' },
];

export function firstPermittedRoute(modulosPermitidos: Modulo[]): string {
  const found = MODULO_ROUTE_ORDER.find((r) => modulosPermitidos.includes(r.modulo));
  return found?.path ?? '/login';
}
