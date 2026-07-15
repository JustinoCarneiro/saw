import type { CategoriaAviso } from './types';

// Fonte única pro ícone/cor/label de cada categoria de aviso — usado por AvisosPage e pelo widget
// "Avisos importantes" do DashboardMentoradoPage. Cores fiéis ao protótipo congelado
// (design/prototipo/index.html, array `avisos`): Mentorias=info, Materiais=gold, Eventos=danger.
// GERAL não tem exemplo lá (nenhum aviso "Geral" no seed do protótipo) — usa --violet, a cor
// reservada no design system pra uma 4ª categoria distinta das outras 3 (ver tokens.css).
export const CATEGORIA_ICONE: Record<CategoriaAviso, string> = {
  GERAL: '📢', MENTORIAS: '🎓', MATERIAIS: '📄', EVENTOS: '📅',
};

export const CATEGORIA_LABEL: Record<CategoriaAviso, string> = {
  GERAL: 'Geral', MENTORIAS: 'Mentorias', MATERIAIS: 'Materiais', EVENTOS: 'Eventos',
};

export const CATEGORIA_COR: Record<CategoriaAviso, { bg: string; color: string }> = {
  GERAL: { bg: 'var(--violet-bg)', color: 'var(--violet)' },
  MENTORIAS: { bg: 'var(--info-bg)', color: 'var(--info)' },
  MATERIAIS: { bg: 'var(--warning-bg)', color: 'var(--gold)' },
  EVENTOS: { bg: 'var(--danger-bg)', color: 'var(--danger)' },
};
