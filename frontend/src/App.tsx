import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminShell } from './app/AdminShell';
import { AdminIndexRedirect } from './app/AdminIndexRedirect';
import { PlaceholderScreen, RequireModulo } from './app/RequireModulo';
import { LoginPage } from './features/auth/LoginPage';
import { SolicitarAcessoPage } from './features/auth/SolicitarAcessoPage';
import { TeamPage } from './features/team/TeamPage';
import { ConsolidatedPage } from './features/consolidated/ConsolidatedPage';
import { FinanceiroShell } from './features/financeiro/FinanceiroShell';
import { DashboardFaturamentoPage } from './features/financeiro/DashboardFaturamentoPage';
import { DrePage } from './features/financeiro/DrePage';
import { LancamentosPage } from './features/financeiro/LancamentosPage';
import { ContasPage } from './features/financeiro/ContasPage';
import { ComercialShell } from './features/comercial/ComercialShell';
import { DashboardComercialPage } from './features/comercial/DashboardComercialPage';
import { LeadsComercialPage } from './features/comercial/LeadsComercialPage';
import { RankingComercialPage } from './features/comercial/RankingComercialPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/solicitar-acesso" element={<SolicitarAcessoPage />} />

      <Route path="/admin" element={<AdminShell />}>
        <Route index element={<AdminIndexRedirect />} />
        <Route
          path="dashboard"
          element={
            <RequireModulo modulo="DASHBOARD">
              <PlaceholderScreen title="Dashboard" />
            </RequireModulo>
          }
        />
        <Route
          path="comercial"
          element={
            <RequireModulo modulo="COMERCIAL">
              <ComercialShell />
            </RequireModulo>
          }
        >
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardComercialPage />} />
          <Route path="leads" element={<LeadsComercialPage />} />
          <Route path="ranking" element={<RankingComercialPage />} />
        </Route>
        <Route
          path="financeiro"
          element={
            <RequireModulo modulo="FINANCEIRO">
              <FinanceiroShell />
            </RequireModulo>
          }
        >
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardFaturamentoPage />} />
          <Route path="dre" element={<DrePage />} />
          <Route path="lancamentos" element={<LancamentosPage />} />
          <Route path="contas" element={<ContasPage />} />
        </Route>
        <Route
          path="mentorados"
          element={
            <RequireModulo modulo="MENTORADOS">
              <PlaceholderScreen title="Mentorados" />
            </RequireModulo>
          }
        />
        <Route
          path="consolidado"
          element={
            <RequireModulo modulo="PAINEL_CONSOLIDADO">
              <ConsolidatedPage />
            </RequireModulo>
          }
        />
        <Route
          path="time"
          element={
            <RequireModulo modulo="TIME">
              <TeamPage />
            </RequireModulo>
          }
        />
        <Route
          path="conteudos"
          element={
            <RequireModulo modulo="CONTEUDOS">
              <PlaceholderScreen title="Conteúdos" />
            </RequireModulo>
          }
        />
      </Route>

      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
