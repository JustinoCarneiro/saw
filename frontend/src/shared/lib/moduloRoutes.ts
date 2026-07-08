import type { Modulo } from './types';

// Mesma ordem de prioridade da sidebar — usada pra decidir pra onde mandar o usuário
// logo após o login (primeiro módulo que a área dele realmente permite).
export const MODULO_ROUTE_ORDER: { modulo: Modulo; path: string }[] = [
  { modulo: 'DASHBOARD', path: '/admin/dashboard' },
  { modulo: 'COMERCIAL', path: '/admin/comercial' },
  { modulo: 'FINANCEIRO', path: '/admin/financeiro' },
  { modulo: 'MENTORADOS', path: '/admin/mentorados' },
  { modulo: 'PAINEL_CONSOLIDADO', path: '/admin/consolidado' },
  { modulo: 'TIME', path: '/admin/time' },
  { modulo: 'CONTEUDOS', path: '/admin/conteudos' },
];

export function firstPermittedRoute(modulosPermitidos: Modulo[]): string {
  const found = MODULO_ROUTE_ORDER.find((r) => modulosPermitidos.includes(r.modulo));
  return found?.path ?? '/login';
}
