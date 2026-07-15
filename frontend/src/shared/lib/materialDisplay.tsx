import type { ReactElement } from 'react';
import { ICON_PROPS } from '../components/iconProps';
import type { TipoConteudo } from './types';

// Ícone por tipo de conteúdo (MateriaisPage) — mesmo achado de UX do avisoDisplay.tsx: emoji
// (📄📊📽️▶️) destoava do resto do app. Sem referência de cor por tipo (ao contrário de
// CategoriaAviso, TipoConteudo não tem exemplo colorido no protótipo congelado), então o selo
// mantém o fundo neutro que já existia (var(--elevated), ver .materialIcon em
// MateriaisPage.module.css) — só o glifo interno muda de emoji pra SVG (ICON_PROPS).
const ICONE_DOCUMENTO = (
  <svg {...ICON_PROPS} width={18} height={18}>
    <path d="M4 5h11a2 2 0 0 1 2 2v14H6a2 2 0 0 1-2-2Z" />
    <path d="M8 9h6M8 13h6M8 17h4" />
  </svg>
);
const ICONE_PLANILHA = (
  <svg {...ICON_PROPS} width={18} height={18}>
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <line x1="3" y1="9" x2="21" y2="9" />
    <line x1="3" y1="15" x2="21" y2="15" />
    <line x1="9" y1="3" x2="9" y2="21" />
  </svg>
);
const ICONE_APRESENTACAO = (
  <svg {...ICON_PROPS} width={18} height={18}>
    <rect x="3" y="4" width="18" height="12" rx="1.5" />
    <path d="M8 20h8M12 16v4" />
  </svg>
);
const ICONE_VIDEO = (
  <svg {...ICON_PROPS} width={18} height={18}>
    <circle cx="12" cy="12" r="9" />
    <path d="M10 8.5v7l6-3.5-6-3.5Z" fill="currentColor" stroke="none" />
  </svg>
);
// OUTRO — clipe, mesmo conceito do 📎 que substitui (formato não categorizado nos 4 tipos acima).
const ICONE_OUTRO = (
  <svg {...ICON_PROPS} width={18} height={18}>
    <path d="M8 12.5V7a4 4 0 0 1 8 0v9a2.5 2.5 0 0 1-5 0V8.5" />
  </svg>
);

export const TIPO_ICONE: Record<TipoConteudo, ReactElement> = {
  DOCUMENTO: ICONE_DOCUMENTO,
  PLANILHA: ICONE_PLANILHA,
  APRESENTACAO: ICONE_APRESENTACAO,
  VIDEO: ICONE_VIDEO,
  OUTRO: ICONE_OUTRO,
};
