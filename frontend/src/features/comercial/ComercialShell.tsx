import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Topbar } from '../../shared/components/Topbar';
import { areaLabel } from '../../shared/components/Pill';
import styles from './ComercialShell.module.css';

const TABS = [
  { to: '/admin/comercial/dashboard', label: 'Dashboard' },
  { to: '/admin/comercial/leads', label: 'Funil de vendas' },
  { to: '/admin/comercial/ranking', label: 'Metas e ranking' },
  // Loja — Produtos / Loja — Pedidos: pausadas (ver docs/reuniao-2026-07-17-atualizacoes.md),
  // ficam fora da navegação enquanto LOJA_ADMIN_PAUSADA=true; rotas seguem existindo com tela
  // de aviso, ver App.tsx.
];

export function ComercialShell() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className={styles.page}>
      <Topbar
        title="Comercial"
        subtitle="Leads, funil de vendas, faturamento e desempenho do time."
        userName={user.nome}
        userRole={areaLabel(user.area ?? '')}
      />
      <div className={styles.tabs}>
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) => `${styles.tab} ${isActive ? styles.tabActive : ''}`}
          >
            {tab.label}
          </NavLink>
        ))}
      </div>
      <div className={styles.content}>
        <Outlet />
      </div>
    </div>
  );
}
