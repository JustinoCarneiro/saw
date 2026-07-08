import { Navigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';
import { firstPermittedRoute } from '../shared/lib/moduloRoutes';

export function AdminIndexRedirect() {
  const { user } = useAuth();
  return <Navigate to={firstPermittedRoute(user?.modulosPermitidos ?? [])} replace />;
}
