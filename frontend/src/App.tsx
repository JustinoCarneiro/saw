import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminShell } from './app/AdminShell';
import { AdminIndexRedirect } from './app/AdminIndexRedirect';
import { MentoradoShell } from './app/MentoradoShell';
import { RequireModulo } from './app/RequireModulo';
import { PausedScreen } from './shared/components/PausedScreen';
import { LOJA_ADMIN_PAUSADA } from './shared/lib/featureFlags';
import { LoginPage } from './features/auth/LoginPage';
import { SolicitarAcessoPage } from './features/auth/SolicitarAcessoPage';
import { EsqueciSenhaPage } from './features/auth/EsqueciSenhaPage';
import { RedefinirSenhaPage } from './features/auth/RedefinirSenhaPage';
import { TeamPage } from './features/team/TeamPage';
import { ConsolidatedPage } from './features/consolidated/ConsolidatedPage';
import { DashboardAdminPage } from './features/dashboard/DashboardAdminPage';
import { FinanceiroShell } from './features/financeiro/FinanceiroShell';
import { DashboardFaturamentoPage } from './features/financeiro/DashboardFaturamentoPage';
import { DrePage } from './features/financeiro/DrePage';
import { LancamentosPage } from './features/financeiro/LancamentosPage';
import { ContasPage } from './features/financeiro/ContasPage';
import { ConciliacaoPage } from './features/financeiro/ConciliacaoPage';
import { ComercialShell } from './features/comercial/ComercialShell';
import { DashboardComercialPage } from './features/comercial/DashboardComercialPage';
import { LeadsComercialPage } from './features/comercial/LeadsComercialPage';
import { RankingComercialPage } from './features/comercial/RankingComercialPage';
import { ProdutosPage } from './features/comercial/ProdutosPage';
import { PedidosPage } from './features/comercial/PedidosPage';
import { MentoradosShell } from './features/mentorados/MentoradosShell';
import { MentoradosListaPage } from './features/mentorados/MentoradosListaPage';
import { MentoriasAgendaPage } from './features/mentorados/MentoriasAgendaPage';
import { MetasAdminPage } from './features/mentorados/MetasAdminPage';
import { TarefasAdminPage } from './features/mentorados/TarefasAdminPage';
import { AtaDetalhePage } from './features/mentorados/AtaDetalhePage';
import { ConteudosShell } from './features/conteudos/ConteudosShell';
import { ConteudosPage } from './features/conteudos/ConteudosPage';
import { EventosPage } from './features/conteudos/EventosPage';
import { DashboardMentoradoPage } from './features/mentorado/DashboardMentoradoPage';
import { MetasPage } from './features/mentorado/MetasPage';
import { TarefasPage } from './features/mentorado/TarefasPage';
import { MateriaisPage } from './features/mentorado/MateriaisPage';
import { MentoriasPage } from './features/mentorado/MentoriasPage';
import { EventosMentoradoPage } from './features/mentorado/EventosMentoradoPage';
import { LojaPage } from './features/mentorado/LojaPage';
import { PerfilPage } from './features/mentorado/PerfilPage';
import { AvisosPage } from './features/mentorado/AvisosPage';
import { AvisosAdminPage } from './features/conteudos/AvisosAdminPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/solicitar-acesso" element={<SolicitarAcessoPage />} />
      <Route path="/esqueci-senha" element={<EsqueciSenhaPage />} />
      <Route path="/redefinir-senha" element={<RedefinirSenhaPage />} />

      <Route path="/admin" element={<AdminShell />}>
        <Route index element={<AdminIndexRedirect />} />
        <Route
          path="dashboard"
          element={
            <RequireModulo modulo="DASHBOARD">
              <DashboardAdminPage />
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
          <Route
            path="produtos"
            element={
              LOJA_ADMIN_PAUSADA ? (
                <PausedScreen
                  title="Loja pausada"
                  description="A Loja SAW está pausada no momento — foco atual é Comercial, Financeiro e Mentorados."
                />
              ) : (
                <ProdutosPage />
              )
            }
          />
          <Route
            path="pedidos"
            element={
              LOJA_ADMIN_PAUSADA ? (
                <PausedScreen
                  title="Loja pausada"
                  description="A Loja SAW está pausada no momento — foco atual é Comercial, Financeiro e Mentorados."
                />
              ) : (
                <PedidosPage />
              )
            }
          />
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
          <Route path="conciliacao" element={<ConciliacaoPage />} />
        </Route>
        <Route
          path="mentorados"
          element={
            <RequireModulo modulo="MENTORADOS">
              <MentoradosShell />
            </RequireModulo>
          }
        >
          <Route index element={<Navigate to="consolidado" replace />} />
          <Route path="consolidado" element={<ConsolidatedPage />} />
          <Route path="lista" element={<MentoradosListaPage />} />
          <Route path="mentorias" element={<MentoriasAgendaPage />} />
          <Route path="metas" element={<MetasAdminPage />} />
          <Route path="tarefas" element={<TarefasAdminPage />} />
          <Route path="mentorias/:mentoriaId/ata" element={<AtaDetalhePage />} />
        </Route>
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
          <Route path="avisos" element={<AvisosAdminPage />} />
        </Route>
      </Route>

      <Route path="/mentorado" element={<MentoradoShell />}>
        <Route index element={<DashboardMentoradoPage />} />
        <Route path="metas" element={<MetasPage />} />
        <Route path="tarefas" element={<TarefasPage />} />
        <Route path="materiais" element={<MateriaisPage />} />
        <Route path="mentorias" element={<MentoriasPage />} />
        <Route path="eventos" element={<EventosMentoradoPage />} />
        <Route path="loja" element={<LojaPage />} />
        <Route path="avisos" element={<AvisosPage />} />
        <Route path="perfil" element={<PerfilPage />} />
      </Route>

      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
