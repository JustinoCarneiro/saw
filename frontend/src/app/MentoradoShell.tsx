import { useEffect, useState } from 'react';
import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import logoSawIcon from '../assets/logo-saw-icon.png';
import { useAuth } from '../features/auth/AuthContext';
import { Avatar } from '../shared/components/Avatar';
import { ICON_PROPS } from '../shared/components/iconProps';
import { apiClient } from '../shared/lib/apiClient';
import type { ResumoAvisos } from '../shared/lib/types';
import styles from './MentoradoShell.module.css';

const NAV_ITEMS = [
  { to: '/mentorado', label: 'Dashboard', end: true },
  { to: '/mentorado/metas', label: 'Metas', end: false },
  { to: '/mentorado/tarefas', label: 'Tarefas', end: false },
  { to: '/mentorado/mentorias', label: 'Mentorias & Atas', end: false },
  { to: '/mentorado/materiais', label: 'Materiais & Dicas', end: false },
  { to: '/mentorado/eventos', label: 'Eventos', end: false },
  { to: '/mentorado/loja', label: 'Loja SAW', end: false },
  { to: '/mentorado/avisos', label: 'Avisos', end: false },
  { to: '/mentorado/perfil', label: 'Perfil', end: false },
];

// M08 — primeira rota de verdade do perfil MENTORADO (antes disso só existia um placeholder
// pós-login). M09 acrescentou Metas, M10 Tarefas, M12 Mentorias & Atas (posicionado antes de
// Materiais nesta lista pra bater com a ordem pretendida pelo CLAUDE.md § MVP —
// Dashboard → Metas/Tarefas → Mentorias → resto — mesmo Materiais/E6 tendo sido construído
// primeiro nesta esteira, ver ROADMAP.md M11/M12), M13 Eventos ("resto" — CLAUDE.md não define
// ordem dentro do bucket, entra depois de Materiais na mesma ordem de construção). Não reusa
// Sidebar (é exclusiva do Modulo/RBAC por área do Admin/E15 — Mentorado não tem área).
export function MentoradoShell() {
  const { user, loading, logout } = useAuth();
  const location = useLocation();
  const [naoLidos, setNaoLidos] = useState(0);

  useEffect(() => {
    if (!user) return;
    apiClient.get<ResumoAvisos>('/mentorado/avisos/resumo')
      .then((res) => setNaoLidos(res.data.naoLidos))
      .catch(() => {});
    // Refaz a contagem ao voltar de /mentorado/avisos (onde o mentorado pode ter marcado como
    // lido) — sem websocket/polling, só reagindo à navegação de rota.
  }, [user, location.pathname]);

  if (loading) {
    return <div style={{ padding: 40, color: 'var(--text-soft)' }}>Carregando…</div>;
  }

  if (!user || user.perfil !== 'MENTORADO') {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.brand}>
          <img src={logoSawIcon} alt="SAW" width={28} height={28} />
          <span className={styles.brandWord}>SAW HUB</span>
        </div>
        <div className={styles.right}>
          <NavLink to="/mentorado/avisos" className={styles.bellButton} aria-label="Notificações" data-testid="sino-avisos">
            <svg {...ICON_PROPS}>
              <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
            {naoLidos > 0 && <span className={styles.bellBadge}>{naoLidos}</span>}
          </NavLink>
          <div className={styles.userChip}>
            <Avatar name={user.nome} size={32} />
            <span className={styles.userName}>{user.nome}</span>
          </div>
          <button className={styles.logoutBtn} onClick={logout}>
            Sair
          </button>
        </div>
      </header>
      <nav className={styles.nav}>
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.navItemActive : ''}`}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
