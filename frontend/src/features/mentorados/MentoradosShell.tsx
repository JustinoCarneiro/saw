import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Topbar } from '../../shared/components/Topbar';
import { areaLabel } from '../../shared/components/Pill';
import styles from './MentoradosShell.module.css';

// M28 (change request, 21/07/2026) — "Mentorados" renomeado pra "Gestão de Performance" no nível
// de SEÇÃO (Sidebar + título do Topbar abaixo), nome que o time da SAW já usa internamente. A aba
// "lista" continua "Mentorados" de propósito (achado do Marcos revisando o dev): é a tela de
// CRUD dos registros de Mentorado, não a seção — mesmo critério já usado no botão "Exportar
// Mentorados" (CsvImportExport), que também não virou "Exportar Gestão de Performance".
const TABS = [
  { to: '/admin/mentorados/consolidado', label: 'Painel Consolidado' },
  { to: '/admin/mentorados/lista', label: 'Mentorados' },
  { to: '/admin/mentorados/mentorias', label: 'Mentorias' },
  { to: '/admin/mentorados/metas', label: 'Metas' },
  { to: '/admin/mentorados/tarefas', label: 'Encaminhamentos' },
];

export function MentoradosShell() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className={styles.page}>
      <Topbar
        title="Gestão de Performance"
        subtitle="Cadastro de mentorados, agenda de mentorias e atas."
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
