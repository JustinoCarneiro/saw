import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { Avatar } from '../shared/components/Avatar';
import styles from './MentoradoShell.module.css';

// M08 — primeira rota de verdade do perfil MENTORADO (antes disso só existia um placeholder
// pós-login). Sem navegação multi-página ainda: só o Dashboard existe nesta leva; cresce quando
// E3/E4/etc. entrarem (CLAUDE.md § MVP: Dashboard → Metas/Tarefas → Mentorias → resto). Por isso
// não reusa Sidebar (é exclusiva do Modulo/RBAC por área do Admin/E15 — Mentorado não tem área).
export function MentoradoShell() {
  const { user, loading, logout } = useAuth();

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
          <svg width="28" height="28" viewBox="0 0 44 44" fill="none">
            <rect x="22" y="6" width="22" height="22" rx="3.5" transform="rotate(45 22 6)" stroke="#F0B050" strokeWidth="1.5" />
            <rect x="22" y="14" width="10" height="10" rx="2" transform="rotate(45 22 14)" stroke="#F0B050" strokeWidth="1.3" />
          </svg>
          <span className={styles.brandWord}>SAW HUB</span>
        </div>
        <div className={styles.right}>
          <div className={styles.userChip}>
            <Avatar name={user.nome} size={32} />
            <span className={styles.userName}>{user.nome}</span>
          </div>
          <button className={styles.logoutBtn} onClick={logout}>
            Sair
          </button>
        </div>
      </header>
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
}
