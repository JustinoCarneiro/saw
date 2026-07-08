import type { ReactNode } from 'react';
import { useAuth } from '../features/auth/AuthContext';
import type { Modulo } from '../shared/lib/types';

export function RequireModulo({ modulo, children }: { modulo: Modulo; children: ReactNode }) {
  const { user } = useAuth();
  if (!user?.modulosPermitidos.includes(modulo)) {
    return (
      <div style={{ padding: 40 }}>
        <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 8 }}>Sem acesso</div>
        <div style={{ color: 'var(--text-soft)', fontSize: 14 }}>
          Sua área não tem permissão para acessar este módulo.
        </div>
      </div>
    );
  }
  return <>{children}</>;
}

export function PlaceholderScreen({ title }: { title: string }) {
  return (
    <div
      style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: 60,
        gap: 16,
      }}
    >
      <div style={{ fontSize: 20, fontWeight: 700 }}>{title}</div>
      <div style={{ fontSize: 14, color: 'var(--text-soft)', maxWidth: 400 }}>
        Este módulo ainda não foi construído nesta primeira leva do MVP.
      </div>
    </div>
  );
}
