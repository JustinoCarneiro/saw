import type { ReactElement } from 'react';
import { NavLink } from 'react-router-dom';
import logoSawIcon from '../../assets/logo-saw-icon.png';
import type { Modulo } from '../lib/types';
import { ICON_PROPS } from './iconProps';
import styles from './Sidebar.module.css';

interface NavItem {
  modulo: Modulo;
  to: string;
  label: string;
  icon: ReactElement;
}

const NAV_ITEMS: NavItem[] = [
  {
    modulo: 'DASHBOARD',
    to: '/admin/dashboard',
    label: 'Dashboard',
    icon: (
      <svg {...ICON_PROPS}>
        <rect x="3" y="3" width="7" height="9" rx="1.5" />
        <rect x="14" y="3" width="7" height="5" rx="1.5" />
        <rect x="14" y="12" width="7" height="9" rx="1.5" />
        <rect x="3" y="16" width="7" height="5" rx="1.5" />
      </svg>
    ),
  },
  {
    modulo: 'COMERCIAL',
    to: '/admin/comercial',
    label: 'Comercial',
    icon: (
      <svg {...ICON_PROPS}>
        <path d="M3 17l6-6 4 4 8-8" />
        <path d="M17 7h4v4" />
      </svg>
    ),
  },
  {
    modulo: 'FINANCEIRO',
    to: '/admin/financeiro',
    label: 'Financeiro',
    icon: (
      <svg {...ICON_PROPS}>
        <rect x="2" y="5" width="20" height="14" rx="2" />
        <path d="M2 10h20" />
        <path d="M6 15h4" />
      </svg>
    ),
  },
  {
    modulo: 'MENTORADOS',
    to: '/admin/mentorados',
    // M28 (change request, 21/07/2026) — renomeado de "Mentorados" pra bater com o nome que o
    // time da SAW já usa internamente (mesmo nome do cargo/área "Gestão de Performance" em
    // Colaborador.area, coincidência aceita pelo cliente). Rota não muda, só o label visível.
    label: 'Gestão de Performance',
    icon: (
      <svg {...ICON_PROPS}>
        <circle cx="9" cy="8" r="3" />
        <path d="M3 20c0-3.3 3-5 6-5s6 1.7 6 5" />
        <circle cx="17.5" cy="9" r="2.2" />
        <path d="M17.5 13.5c2 .4 3.5 1.7 3.5 4" />
      </svg>
    ),
  },
  {
    modulo: 'TIME',
    to: '/admin/time',
    label: 'Time',
    icon: (
      <svg {...ICON_PROPS}>
        <rect x="3" y="7" width="18" height="13" rx="2" />
        <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        <path d="M3 12h18" />
      </svg>
    ),
  },
  {
    modulo: 'CONTEUDOS',
    to: '/admin/conteudos',
    label: 'Conteúdos',
    icon: (
      <svg {...ICON_PROPS}>
        <path d="M4 5h11a2 2 0 0 1 2 2v14H6a2 2 0 0 1-2-2Z" />
        <path d="M8 9h6M8 13h6" />
      </svg>
    ),
  },
];

interface SidebarProps {
  modulosPermitidos: Modulo[];
  onLogout: () => void;
}

export function Sidebar({ modulosPermitidos, onLogout }: SidebarProps) {
  const permitted = new Set(modulosPermitidos);
  return (
    <aside className={styles.aside}>
      <div className={styles.brand}>
        <img src={logoSawIcon} alt="SAW" width={34} height={34} />
        <div className={styles.brandWord}>SAW</div>
        <span className={styles.brandTag}>ADMIN</span>
      </div>

      <nav className={styles.nav}>
        {NAV_ITEMS.filter((item) => permitted.has(item.modulo)).map((item) => (
          <NavLink
            key={item.modulo}
            to={item.to}
            className={({ isActive }) => `${styles.navBtn} ${isActive ? styles.navBtnActive : ''}`}
          >
            {item.icon}
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className={styles.spacer} />
      <button className={styles.logoutBtn} onClick={onLogout}>
        <svg {...ICON_PROPS}>
          <path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3" />
          <path d="M10 12H3m0 0 3.5-3.5M3 12l3.5 3.5" />
        </svg>
        Sair
      </button>
    </aside>
  );
}
