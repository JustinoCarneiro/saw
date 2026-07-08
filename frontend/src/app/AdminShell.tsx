import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { Sidebar } from '../shared/components/Sidebar';

export function AdminShell() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return <div style={{ padding: 40, color: 'var(--text-soft)' }}>Carregando…</div>;
  }

  if (!user || user.perfil !== 'ADMIN') {
    return <Navigate to="/login" replace />;
  }

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar modulosPermitidos={user.modulosPermitidos} onLogout={logout} />
      <main style={{ flex: 1, minWidth: 0, background: 'var(--bg)', display: 'flex', flexDirection: 'column' }}>
        <Outlet />
      </main>
    </div>
  );
}
