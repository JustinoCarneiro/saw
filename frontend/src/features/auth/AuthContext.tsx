import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import type { MeResponse } from '../../shared/lib/types';

interface AuthState {
  user: MeResponse | null;
  loading: boolean;
  login: (email: string, senha: string) => Promise<MeResponse>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<MeResponse>('/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email: string, senha: string) => {
    // Garante que o cookie XSRF-TOKEN já exista antes do POST (necessário no primeiro
    // acesso, quando ainda não houve nenhuma requisição pra sessão gerar o cookie).
    await apiClient.get('/auth/me').catch(() => undefined);
    const res = await apiClient.post<MeResponse>('/auth/login', { email, senha });
    setUser(res.data);
    return res.data;
  }, []);

  const logout = useCallback(async () => {
    await apiClient.post('/auth/logout');
    setUser(null);
  }, []);

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return ctx;
}
