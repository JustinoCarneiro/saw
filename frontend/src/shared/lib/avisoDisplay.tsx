import type { ReactElement } from 'react';
import { ICON_PROPS } from '../components/iconProps';
import type { CategoriaAviso } from './types';

// Fonte única pro ícone/cor/label de cada categoria de aviso — usado por AvisosPage e pelo widget
// "Avisos importantes" do DashboardMentoradoPage.
//
// Ícones em SVG (ICON_PROPS, o mesmo padrão linear da Sidebar/DashboardAdminPage — design/
// DESIGN.md §8), não emoji: o protótipo estático congelado (design/prototipo/index.html) usa
// emoji como placeholder nos cards de aviso, mas a implementação React já mais madura de
// atividades recentes (DashboardAdminPage.ATIVIDADE_ICONE) resolveu o mesmo problema — "selo
// colorido com ícone SVG dentro" — sem emoji, e é esse o padrão que vingou no código real. Emoji
// ao lado de ícone linear fino destoa (achado de UX). Reaproveita ícones exatos já usados noutro
// lugar: pessoas (Sidebar "Mentorados"/DashboardAdminPage MENTORADO_CADASTRADO), documento
// (DashboardAdminPage CONTEUDO_PUBLICADO), calendário (DashboardAdminPage EVENTO_CRIADO); sino é
// o único novo, copiado do ícone de navegação "Avisos" do próprio protótipo congelado (linha
// ~170 do index.html).
const ICONE_GERAL = (
  <svg {...ICON_PROPS} width={16} height={16}>
    <path d="M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" />
    <path d="M10 20a2 2 0 0 0 4 0" />
  </svg>
);
const ICONE_MENTORIAS = (
  <svg {...ICON_PROPS} width={16} height={16}>
    <circle cx="9" cy="8" r="3" />
    <path d="M3 20c0-3.3 3-5 6-5s6 1.7 6 5" />
    <circle cx="17.5" cy="9" r="2.2" />
    <path d="M17.5 13.5c2 .4 3.5 1.7 3.5 4" />
  </svg>
);
const ICONE_MATERIAIS = (
  <svg {...ICON_PROPS} width={16} height={16}>
    <path d="M4 5h11a2 2 0 0 1 2 2v14H6a2 2 0 0 1-2-2Z" />
    <path d="M8 9h6M8 13h6" />
  </svg>
);
const ICONE_EVENTOS = (
  <svg {...ICON_PROPS} width={16} height={16} viewBox="0 0 24 24">
    <rect x="3" y="4" width="18" height="18" rx="2" />
    <line x1="16" y1="2" x2="16" y2="6" />
    <line x1="8" y1="2" x2="8" y2="6" />
    <line x1="3" y1="10" x2="21" y2="10" />
  </svg>
);

export const CATEGORIA_ICONE: Record<CategoriaAviso, ReactElement> = {
  GERAL: ICONE_GERAL, MENTORIAS: ICONE_MENTORIAS, MATERIAIS: ICONE_MATERIAIS, EVENTOS: ICONE_EVENTOS,
};

export const CATEGORIA_LABEL: Record<CategoriaAviso, string> = {
  GERAL: 'Geral', MENTORIAS: 'Mentorias', MATERIAIS: 'Materiais', EVENTOS: 'Eventos',
};

// Cores por categoria — fiéis ao protótipo congelado (design/prototipo/index.html, array
// `avisos`): Mentorias=info, Materiais=gold, Eventos=danger. GERAL não tem exemplo lá (nenhum
// aviso "Geral" no seed de lá) — usa --violet, a cor reservada no design system pra uma 4ª
// categoria distinta das outras 3 (ver tokens.css).
export const CATEGORIA_COR: Record<CategoriaAviso, { bg: string; color: string }> = {
  GERAL: { bg: 'var(--violet-bg)', color: 'var(--violet)' },
  MENTORIAS: { bg: 'var(--info-bg)', color: 'var(--info)' },
  MATERIAIS: { bg: 'var(--warning-bg)', color: 'var(--gold)' },
  EVENTOS: { bg: 'var(--danger-bg)', color: 'var(--danger)' },
};
