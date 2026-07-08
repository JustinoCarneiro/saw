import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Topbar } from '../../shared/components/Topbar';
import { areaLabel } from '../../shared/components/Pill';
import styles from './FinanceiroShell.module.css';

const TABS = [
  { to: '/admin/financeiro/dashboard', label: 'Faturamento' },
  { to: '/admin/financeiro/dre', label: 'DRE' },
  { to: '/admin/financeiro/lancamentos', label: 'Lançamentos' },
  { to: '/admin/financeiro/contas', label: 'Contas a pagar/receber' },
];

export function FinanceiroShell() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className={styles.page}>
      <Topbar
        title="Financeiro"
        subtitle="Lançamentos, DRE, faturamento e contas a pagar/receber."
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
