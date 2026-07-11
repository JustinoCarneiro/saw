import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DonutChart } from '../../shared/components/DonutChart';
import { ICON_PROPS } from '../../shared/components/iconProps';
import { LineChart } from '../../shared/components/LineChart';
import { areaLabel } from '../../shared/components/Pill';
import { Topbar } from '../../shared/components/Topbar';
import { formatBRL, formatPct } from '../../shared/lib/format';
import type { DashboardAdminResponse, Plano } from '../../shared/lib/types';
import styles from './DashboardAdminPage.module.css';

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito', BASICO: 'Básico', ESSENCIAL: 'Essencial', PROFISSIONAL: 'Profissional',
};

const PLANO_COLOR: Record<Plano, string> = {
  GRATUITO: 'var(--text-faint)', BASICO: 'var(--info)', ESSENCIAL: 'var(--gold)', PROFISSIONAL: 'var(--success)',
};

// M23 — trocado de emoji (único lugar do sistema que usava; quebrava design/DESIGN.md §8:
// "Linear, traço ~1.6px, cantos arredondados, currentColor") por SVG no mesmo ICON_PROPS da
// Sidebar, dentro de um selo colorido — réplica do mockup (design/mockups-ref/06-admin.png,
// Tela 11), que usa um quadrado arredondado colorido por tipo de atividade, não um ícone solto.
const ATIVIDADE_ICONE: Record<string, JSX.Element> = {
  MENTORADO_CADASTRADO: (
    <svg {...ICON_PROPS} width={15} height={15}>
      <circle cx="9" cy="8" r="3" />
      <path d="M3 20c0-3.3 3-5 6-5s6 1.7 6 5" />
      <circle cx="17.5" cy="9" r="2.2" />
      <path d="M17.5 13.5c2 .4 3.5 1.7 3.5 4" />
    </svg>
  ),
  EVENTO_CRIADO: (
    <svg {...ICON_PROPS} width={15} height={15} viewBox="0 0 24 24">
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  ),
  CONTEUDO_PUBLICADO: (
    <svg {...ICON_PROPS} width={15} height={15}>
      <path d="M4 5h11a2 2 0 0 1 2 2v14H6a2 2 0 0 1-2-2Z" />
      <path d="M8 9h6M8 13h6" />
    </svg>
  ),
};

const ATIVIDADE_COR: Record<string, { bg: string; color: string }> = {
  MENTORADO_CADASTRADO: { bg: 'var(--success-bg)', color: 'var(--success)' },
  EVENTO_CRIADO: { bg: 'var(--warning-bg)', color: 'var(--warning)' },
  CONTEUDO_PUBLICADO: { bg: 'var(--info-bg)', color: 'var(--info)' },
};

function variacaoCor(pct: number): string {
  return pct >= 0 ? 'var(--success)' : 'var(--danger)';
}

function formatarMesCurto(mes: string): string {
  const [, m] = mes.split('-');
  return ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'][Number(m) - 1];
}

function formatarQuando(iso: string): string {
  const diffMin = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (diffMin < 1) return 'agora';
  if (diffMin < 60) return `há ${diffMin} min`;
  const diffH = Math.round(diffMin / 60);
  if (diffH < 24) return `há ${diffH}h`;
  return `há ${Math.round(diffH / 24)}d`;
}

export function DashboardAdminPage() {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState<DashboardAdminResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const now = new Date();
    apiClient
      .get<DashboardAdminResponse>('/admin/dashboard', { params: { ano: now.getFullYear(), mes: now.getMonth() + 1 } })
      .then((res) => setDashboard(res.data))
      .catch(() => setError('Não foi possível carregar o dashboard.'));
  }, []);

  if (!user) return null;

  return (
    <div className={styles.page}>
      <Topbar
        title="Dashboard"
        subtitle="Visão geral da operação: mentorados, mentorias, eventos e receita."
        userName={user.nome}
        userRole={areaLabel(user.area ?? '')}
      />
      <div className={styles.content}>
        {error && <div className={styles.error} data-testid="dashboard-admin-erro">{error}</div>}
        {!error && !dashboard && <div className={styles.loading}>Carregando…</div>}
        {dashboard && <DashboardAdminConteudo dashboard={dashboard} />}
      </div>
    </div>
  );
}

function DashboardAdminConteudo({ dashboard }: { dashboard: DashboardAdminResponse }) {
  const totalDistribuicao = dashboard.distribuicaoPlano.reduce((soma, d) => soma + d.quantidade, 0);

  return (
    <div className={styles.container}>
      <div className={styles.kpis}>
        <Card style={{ padding: 18 }} testId="kpi-mentorados-ativos">
          <div className={styles.kpiLabel}>Mentorados ativos</div>
          <div className={styles.kpiValue}>{dashboard.mentoradosAtivos}</div>
          <div className={styles.kpiHint} style={{ color: variacaoCor(dashboard.variacaoMentoradosAtivosPct) }}>
            {formatPct(dashboard.variacaoMentoradosAtivosPct)} este mês
          </div>
        </Card>
        <Card style={{ padding: 18 }} testId="kpi-mentorias-realizadas">
          <div className={styles.kpiLabel}>Mentorias realizadas</div>
          <div className={styles.kpiValue}>{dashboard.mentoriasRealizadas}</div>
          <div className={styles.kpiHint} style={{ color: variacaoCor(dashboard.variacaoMentoriasRealizadasPct) }}>
            {formatPct(dashboard.variacaoMentoriasRealizadasPct)} este mês
          </div>
        </Card>
        <Card style={{ padding: 18 }} testId="kpi-eventos-realizados">
          <div className={styles.kpiLabel}>Eventos realizados</div>
          <div className={styles.kpiValue}>{dashboard.eventosRealizados}</div>
          <div className={styles.kpiHint} style={{ color: variacaoCor(dashboard.variacaoEventosRealizadosPct) }}>
            {formatPct(dashboard.variacaoEventosRealizadosPct)} este mês
          </div>
        </Card>
        <Card style={{ padding: 18 }} testId="kpi-receita-mes">
          <div className={styles.kpiLabel}>Receita este mês</div>
          <div className={styles.kpiValue}>{formatBRL(dashboard.receitaMes)}</div>
          <div className={styles.kpiHint} style={{ color: variacaoCor(dashboard.variacaoReceitaMesPct) }}>
            {formatPct(dashboard.variacaoReceitaMesPct)} este mês
          </div>
        </Card>
      </div>

      <div className={styles.row}>
        <Card style={{ padding: '20px 22px' }} testId="grafico-crescimento-mentorados">
          <div className={styles.chartHeader}>
            <div className={styles.sectionTitle}>Crescimento de mentorados</div>
            <span className={styles.periodoLabel}>Últimos 6 meses</span>
          </div>
          <LineChart
            pontos={dashboard.crescimentoMentorados.map((c) => ({ chave: c.mes, rotulo: formatarMesCurto(c.mes), valor: c.total }))}
          />
        </Card>

        <Card style={{ padding: '20px 22px' }} testId="grafico-distribuicao-plano">
          <div className={styles.sectionTitle}>Distribuição por plano</div>
          <div className={styles.donutRow}>
            <DonutChart
              segmentos={dashboard.distribuicaoPlano.map((d) => ({ chave: d.plano, valor: d.quantidade, cor: PLANO_COLOR[d.plano] }))}
            />
            <div className={styles.legendaLista}>
              {dashboard.distribuicaoPlano.map((d) => (
                <div key={d.plano} className={styles.legendaItem}>
                  <span className={styles.dot} style={{ background: PLANO_COLOR[d.plano] }} />
                  <span className={styles.legendaLabel}>{PLANO_LABEL[d.plano]}</span>
                  <span className={styles.legendaValor}>
                    {totalDistribuicao === 0 ? '—' : `${d.pct.toFixed(0)}%`}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </Card>
      </div>

      <div className={styles.row}>
        <Card style={{ padding: '20px 22px' }} testId="atividades-recentes">
          <div className={styles.sectionTitle}>Atividades recentes</div>
          {dashboard.atividadesRecentes.length === 0 && <div className={styles.emptyState}>Nenhuma atividade recente.</div>}
          <div className={styles.listaAtividades}>
            {dashboard.atividadesRecentes.map((a, i) => {
              const cor = ATIVIDADE_COR[a.tipo] ?? { bg: 'var(--line)', color: 'var(--text-soft)' };
              return (
                <div key={i} className={styles.atividadeRow}>
                  <span className={styles.atividadeIcone} style={{ background: cor.bg, color: cor.color }}>
                    {ATIVIDADE_ICONE[a.tipo] ?? '•'}
                  </span>
                  <span className={styles.atividadeDescricao}>{a.descricao}</span>
                  <span className={styles.atividadeQuando}>{formatarQuando(a.quando)}</span>
                </div>
              );
            })}
          </div>
        </Card>

        <Card style={{ padding: '20px 22px' }} testId="mentorias-hoje">
          <div className={styles.sectionTitle}>Mentorias agendadas para hoje</div>
          {dashboard.mentoriasHoje.length === 0 && <div className={styles.emptyState}>Nenhuma mentoria hoje.</div>}
          <div className={styles.listaAtividades}>
            {dashboard.mentoriasHoje.map((m, i) => (
              <div key={i} className={styles.mentoriaRow}>
                <div>
                  <div className={styles.mentoriaTitulo}>
                    {m.tipo === 'INDIVIDUAL' ? 'Mentoria Individual' : 'Mentoria em Grupo'}
                  </div>
                  <div className={styles.mentoriaSub}>{m.hora} · com {m.mentoradoNomes}</div>
                </div>
                <span className={styles.statusBadge}>{m.status === 'CONFIRMADA' ? 'Confirmada' : 'Agendada'}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
