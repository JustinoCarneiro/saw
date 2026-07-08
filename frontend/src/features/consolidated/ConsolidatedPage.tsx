import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../../shared/lib/apiClient';
import { Avatar } from '../../shared/components/Avatar';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { StatusPill, areaLabel } from '../../shared/components/Pill';
import { Topbar } from '../../shared/components/Topbar';
import type { ConsolidatedSummary, MentoradoConsolidado, RankingFaturamento } from '../../shared/lib/types';
import styles from './ConsolidatedPage.module.css';

const COLUMNS = '1.8fr .8fr 1fr 1fr .9fr .9fr';

function pctColor(pct: number): string {
  return pct >= 0 ? 'var(--success)' : 'var(--danger)';
}

function formatPct(pct: number): string {
  const sign = pct > 0 ? '+' : '';
  return `${sign}${pct}%`;
}

export function ConsolidatedPage() {
  const { user } = useAuth();
  const [mentorados, setMentorados] = useState<MentoradoConsolidado[] | null>(null);
  const [summary, setSummary] = useState<ConsolidatedSummary | null>(null);
  const [ranking, setRanking] = useState<RankingFaturamento[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      apiClient.get<MentoradoConsolidado[]>('/admin/consolidated/mentorados'),
      apiClient.get<ConsolidatedSummary>('/admin/consolidated/summary'),
      apiClient.get<RankingFaturamento[]>('/admin/consolidated/ranking-faturamento', { params: { top: 3 } }),
    ])
      .then(([mentoradosRes, summaryRes, rankingRes]) => {
        setMentorados(mentoradosRes.data);
        setSummary(summaryRes.data);
        setRanking(rankingRes.data);
      })
      .catch(() => setError('Não foi possível carregar o painel consolidado.'));
  }, []);

  if (!user) return null;

  return (
    <div className={styles.page}>
      <Topbar
        title="Painel Consolidado"
        subtitle="Progresso, encaminhamentos e ranking de todos os mentorados ao mesmo tempo."
        userName={user.nome}
        userRole={areaLabel(user.area ?? '')}
      />

      <div className={styles.content}>
        {error && <div className={styles.error}>{error}</div>}

        {summary && (
          <div className={styles.kpis}>
            <Card style={{ padding: '16px 18px' }}>
              <div className={styles.kpiLabel}>Em dia</div>
              <div className={styles.kpiValue} style={{ color: 'var(--success)' }}>
                {summary.emDia}
              </div>
              <div className={styles.kpiHint}>de {summary.total} mentorados</div>
            </Card>
            <Card style={{ padding: '16px 18px' }}>
              <div className={styles.kpiLabel}>Em atenção</div>
              <div className={styles.kpiValue} style={{ color: 'var(--warning)' }}>
                {summary.atencao}
              </div>
              <div className={styles.kpiHint}>de {summary.total} mentorados</div>
            </Card>
            <Card style={{ padding: '16px 18px' }}>
              <div className={styles.kpiLabel}>Atrasados</div>
              <div className={styles.kpiValue} style={{ color: 'var(--danger)' }}>
                {summary.atrasado}
              </div>
              <div className={styles.kpiHint}>de {summary.total} mentorados</div>
            </Card>
            <Card style={{ padding: '16px 18px' }}>
              <div className={styles.kpiLabel}>Progresso médio</div>
              <div className={styles.kpiValue} style={{ color: 'var(--info)' }}>
                {summary.progressoMedioPct}%
              </div>
              <div className={styles.kpiHint}>encaminhamentos ponderados por peso</div>
            </Card>
          </div>
        )}

        <div className={styles.layout}>
          <DataGrid columns={COLUMNS} headers={['Mentorado', 'Progresso', 'Encaminhamentos', 'Ferramentas', 'Faturamento', 'Status']}>
            {mentorados === null && !error && <div className={styles.loading}>Carregando…</div>}
            {mentorados?.map((m) => (
              <DataGridRow key={m.id} columns={COLUMNS}>
                <div className={styles.person}>
                  <Avatar name={m.nome} size={34} />
                  <div className={styles.personText}>
                    <div className={styles.personName}>{m.nome}</div>
                    <div className={styles.personBiz}>{m.negocio ?? '—'}</div>
                  </div>
                </div>
                <div className={styles.metricStrong}>{m.progressoPct}%</div>
                <div className={styles.metric}>
                  {m.encaminhamentosCumpridos}/{m.encaminhamentosTotal}
                </div>
                <div className={styles.metric}>{m.ferramentasPct}%</div>
                <div className={styles.metricStrong} style={{ color: pctColor(m.crescimentoFaturamentoPct) }}>
                  {formatPct(m.crescimentoFaturamentoPct)}
                </div>
                <div>
                  <StatusPill status={m.status} />
                </div>
              </DataGridRow>
            ))}
          </DataGrid>

          <Card style={{ padding: '20px 22px', height: 'fit-content' }}>
            <div className={styles.rankingTitle}>Ranking · Crescimento de Faturamento</div>
            <div className={styles.rankingSubtitle}>Comparado ao período anterior</div>
            <div className={styles.rankingList}>
              {ranking?.map((r) => (
                <div key={r.pos} className={styles.rankingRow}>
                  <div className={styles.rankingPos}>{r.pos}</div>
                  <Avatar name={r.nome} size={32} />
                  <div className={styles.rankingName}>{r.nome}</div>
                  <div className={styles.rankingValue}>{formatPct(r.crescimentoFaturamentoPct)}</div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
