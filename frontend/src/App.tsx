import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminShell } from './app/AdminShell';
import { AdminIndexRedirect } from './app/AdminIndexRedirect';
import { MentoradoShell } from './app/MentoradoShell';
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
import { MentoradosShell } from './features/mentorados/MentoradosShell';
import { MentoradosListaPage } from './features/mentorados/MentoradosListaPage';
import { MentoriasAgendaPage } from './features/mentorados/MentoriasAgendaPage';
import { AtaDetalhePage } from './features/mentorados/AtaDetalhePage';
import { ConteudosShell } from './features/conteudos/ConteudosShell';
import { ConteudosPage } from './features/conteudos/ConteudosPage';
import { EventosPage } from './features/conteudos/EventosPage';
import { DashboardMentoradoPage } from './features/mentorado/DashboardMentoradoPage';
import { MetasPage } from './features/mentorado/MetasPage';
import { TarefasPage } from './features/mentorado/TarefasPage';
import { MateriaisPage } from './features/mentorado/MateriaisPage';

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
              <MentoradosShell />
            </RequireModulo>
          }
        >
          <Route index element={<Navigate to="lista" replace />} />
          <Route path="lista" element={<MentoradosListaPage />} />
          <Route path="mentorias" element={<MentoriasAgendaPage />} />
          <Route path="mentorias/:mentoriaId/ata" element={<AtaDetalhePage />} />
        </Route>
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
              <ConteudosShell />
            </RequireModulo>
          }
        >
          <Route index element={<Navigate to="lista" replace />} />
          <Route path="lista" element={<ConteudosPage />} />
          <Route path="eventos" element={<EventosPage />} />
        </Route>
      </Route>

      <Route path="/mentorado" element={<MentoradoShell />}>
        <Route index element={<DashboardMentoradoPage />} />
        <Route path="metas" element={<MetasPage />} />
        <Route path="tarefas" element={<TarefasPage />} />
        <Route path="materiais" element={<MateriaisPage />} />
      </Route>

      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
