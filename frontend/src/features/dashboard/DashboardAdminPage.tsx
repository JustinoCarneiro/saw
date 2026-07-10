import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { formatBRL, formatPct } from '../../shared/lib/format';
import type { DashboardAdminResponse, Plano } from '../../shared/lib/types';
import styles from './DashboardAdminPage.module.css';

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito', BASICO: 'Básico', ESSENCIAL: 'Essencial', PROFISSIONAL: 'Profissional',
};

const PLANO_COLOR: Record<Plano, string> = {
  GRATUITO: 'var(--text-faint)', BASICO: 'var(--info)', ESSENCIAL: 'var(--gold)', PROFISSIONAL: 'var(--success)',
};

const ATIVIDADE_ICONE: Record<string, string> = {
  MENTORADO_CADASTRADO: '👤', EVENTO_CRIADO: '📅', CONTEUDO_PUBLICADO: '📄',
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
  const [dashboard, setDashboard] = useState<DashboardAdminResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const now = new Date();
    apiClient
      .get<DashboardAdminResponse>('/admin/dashboard', { params: { ano: now.getFullYear(), mes: now.getMonth() + 1 } })
      .then((res) => setDashboard(res.data))
      .catch(() => setError('Não foi possível carregar o dashboard.'));
  }, []);

  if (error) {
    return <div className={styles.error} data-testid="dashboard-admin-erro">{error}</div>;
  }

  if (!dashboard) {
    return <div className={styles.loading}>Carregando…</div>;
  }

  const maiorCrescimento = Math.max(1, ...dashboard.crescimentoMentorados.map((c) => c.total));

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
        <Card style={{ padding: '20px 22px' }}>
          <div className={styles.sectionTitle}>Crescimento de mentorados</div>
          <div className={styles.barChart}>
            {dashboard.crescimentoMentorados.map((c) => (
              <div key={c.mes} className={styles.barColumn}>
                <div className={styles.barTrack}>
                  <div className={styles.barFill} style={{ height: `${(c.total / maiorCrescimento) * 100}%` }} />
                </div>
                <div className={styles.barValue}>{c.total}</div>
                <div className={styles.barLabel}>{formatarMesCurto(c.mes)}</div>
              </div>
            ))}
          </div>
        </Card>

        <Card style={{ padding: '20px 22px' }}>
          <div className={styles.sectionTitle}>Distribuição por plano</div>
          <div className={styles.funilList}>
            {dashboard.distribuicaoPlano.map((d) => (
              <div key={d.plano} className={styles.funilRow}>
                <div className={styles.funilHeader}>
                  <span className={styles.funilLabel}>
                    <span className={styles.dot} style={{ background: PLANO_COLOR[d.plano] }} />
                    {PLANO_LABEL[d.plano]}
                  </span>
                  <span className={styles.funilValue}>{d.quantidade} ({d.pct.toFixed(0)}%)</span>
                </div>
                <div className={styles.track}>
                  <div className={styles.fill} style={{ width: `${d.pct}%`, background: PLANO_COLOR[d.plano] }} />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div className={styles.row}>
        <Card style={{ padding: '20px 22px' }} testId="atividades-recentes">
          <div className={styles.sectionTitle}>Atividades recentes</div>
          {dashboard.atividadesRecentes.length === 0 && <div className={styles.emptyState}>Nenhuma atividade recente.</div>}
          <div className={styles.listaAtividades}>
            {dashboard.atividadesRecentes.map((a, i) => (
              <div key={i} className={styles.atividadeRow}>
                <span className={styles.atividadeIcone}>{ATIVIDADE_ICONE[a.tipo] ?? '•'}</span>
                <span className={styles.atividadeDescricao}>{a.descricao}</span>
                <span className={styles.atividadeQuando}>{formatarQuando(a.quando)}</span>
              </div>
            ))}
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
