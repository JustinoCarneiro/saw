// Padrão de ícone do sistema (design/DESIGN.md §8): linear, traço ~1.6px, currentColor, cantos
// arredondados. Segunda vez que este objeto é usado (Sidebar + DashboardAdminPage) — extraído
// pra evitar uma terceira cópia divergir do padrão documentado.
export const ICON_PROPS = {
  width: 19,
  height: 19,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.6,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
};
