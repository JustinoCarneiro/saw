import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../../shared/lib/apiClient';
import { Avatar } from '../../shared/components/Avatar';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { DonutChart } from '../../shared/components/DonutChart';
import { StatusPill, areaLabel } from '../../shared/components/Pill';
import { Topbar } from '../../shared/components/Topbar';
import type { ConsolidatedSummary, MentoradoConsolidado, RankingFaturamento } from '../../shared/lib/types';
import styles from './ConsolidatedPage.module.css';

const COLUMNS = '1.8fr .8fr 1fr 1fr .9fr .9fr';

// M23 — ícones + abas de filtro por status existiam no protótipo estático congelado
// (design/prototipo/index.html, bloco "ADMIN PAINEL CONSOLIDADO") e não sobreviveram à
// portagem pra React — restaurados aqui fielmente ao HTML original (mesmos ícones, mesmas cores).
const KPI_ICON = {
  emDia: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 12l5 5L20 6" />
    </svg>
  ),
  atencao: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--warning)" strokeWidth="1.6">
      <circle cx="12" cy="12" r="8" />
    </svg>
  ),
  atrasado: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--danger)" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 9v4M12 17h.01" />
      <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" />
    </svg>
  ),
  progresso: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--info)" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 3v18h18" />
      <path d="M18 17V9M13 17v-5M8 17v-3" />
    </svg>
  ),
};

type FiltroStatus = 'TODOS' | 'EM_DIA' | 'ATENCAO' | 'ATRASADO';

const ABAS: { valor: FiltroStatus; label: string }[] = [
  { valor: 'TODOS', label: 'Todos' },
  { valor: 'EM_DIA', label: 'Em dia' },
  { valor: 'ATENCAO', label: 'Em atenção' },
  { valor: 'ATRASADO', label: 'Atrasados' },
];

// Mesmas cores já usadas no StatusPill (shared/components/Pill.tsx) pros mesmos status — o
// donut só visualiza a mesma informação dos KPIs acima, não introduz uma paleta nova.
const STATUS_DONUT = [
  { chave: 'EM_DIA', label: 'Em dia', cor: 'var(--success)' },
  { chave: 'ATENCAO', label: 'Em atenção', cor: 'var(--warning)' },
  { chave: 'ATRASADO', label: 'Atrasados', cor: 'var(--danger)' },
] as const;

const STATUS_SUMMARY_KEY: Record<(typeof STATUS_DONUT)[number]['chave'], keyof ConsolidatedSummary> = {
  EM_DIA: 'emDia',
  ATENCAO: 'atencao',
  ATRASADO: 'atrasado',
};

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
  const [filtro, setFiltro] = useState<FiltroStatus>('TODOS');

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

  const mentoradosFiltrados = useMemo(() => {
    if (!mentorados) return mentorados;
    if (filtro === 'TODOS') return mentorados;
    return mentorados.filter((m) => m.status === filtro);
  }, [mentorados, filtro]);

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
              <div className={styles.kpiHeader}>
                <span className={styles.kpiBadge} style={{ background: 'var(--success-bg)' }}>{KPI_ICON.emDia}</span>
                <span className={styles.kpiLabel}>Em dia</span>
              </div>
              <div className={styles.kpiValue} style={{ color: 'var(--success)' }}>
                {summary.emDia}
              </div>
              <div className={styles.kpiHint}>de {summary.total} mentorados</div>
            </Card>
            <Card style={{ padding: '16px 18px' }}>
              <div className={styles.kpiHeader}>
                <span className={styles.kpiBadge} style={{ background: 'var(--warning-bg)' }}>{KPI_ICON.atencao}</span>
                <span className={styles.kpiLabel}>Em atenção</span>
              </div>
              <div className={styles.kpiValue} style={{ color: 'var(--warning)' }}>
                {summary.atencao}
              </div>
              <div className={styles.kpiHint}>de {summary.total} mentorados</div>
            </Card>
            <Card style={{ padding: '16px 18px' }}>
              <div className={styles.kpiHeader}>
                <span className={styles.kpiBadge} style={{ background: 'var(--danger-bg)' }}>{KPI_ICON.atrasado}</span>
                <span className={styles.kpiLabel}>Atrasados</span>
              </div>
              <div className={styles.kpiValue} style={{ color: 'var(--danger)' }}>
                {summary.atrasado}
              </div>
              <div className={styles.kpiHint}>de {summary.total} mentorados</div>
            </Card>
            <Card style={{ padding: '16px 18px' }}>
              <div className={styles.kpiHeader}>
                <span className={styles.kpiBadge} style={{ background: 'var(--info-bg)' }}>{KPI_ICON.progresso}</span>
                <span className={styles.kpiLabel}>Progresso médio</span>
              </div>
              <div className={styles.kpiValue} style={{ color: 'var(--info)' }}>
                {summary.progressoMedioPct}%
              </div>
              <div className={styles.kpiHint}>encaminhamentos ponderados por peso</div>
            </Card>
          </div>
        )}

        <div className={styles.tabs} role="tablist">
          {ABAS.map((aba) => (
            <button
              key={aba.valor}
              type="button"
              role="tab"
              aria-selected={filtro === aba.valor}
              className={`${styles.tab} ${filtro === aba.valor ? styles.tabActive : ''}`}
              onClick={() => setFiltro(aba.valor)}
            >
              {aba.label}
            </button>
          ))}
        </div>

        <div className={styles.layout}>
          <DataGrid columns={COLUMNS} headers={['Mentorado', 'Progresso', 'Encaminhamentos', 'Ferramentas', 'Faturamento', 'Status']}>
            {mentoradosFiltrados === null && !error && <div className={styles.loading}>Carregando…</div>}
            {mentoradosFiltrados?.length === 0 && (
              <div className={styles.loading}>Nenhum mentorado nesse status.</div>
            )}
            {mentoradosFiltrados?.map((m) => (
              <DataGridRow key={m.id} columns={COLUMNS} testId="mentorado-row">
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

          <div className={styles.rightCol}>
            {summary && (
              <Card style={{ padding: '20px 22px' }} testId="grafico-distribuicao-status">
                <div className={styles.sectionTitle}>Distribuição por status</div>
                <div className={styles.donutRow}>
                  <DonutChart
                    titulo="Distribuição por status"
                    tamanho={128}
                    segmentos={STATUS_DONUT.map((s) => ({ chave: s.chave, valor: summary[STATUS_SUMMARY_KEY[s.chave]], cor: s.cor }))}
                  />
                  <div className={styles.legendaLista}>
                    {STATUS_DONUT.map((s) => (
                      <div key={s.chave} className={styles.legendaItem}>
                        <span className={styles.dot} style={{ background: s.cor }} />
                        <span className={styles.legendaLabel}>{s.label}</span>
                        <span className={styles.legendaValor}>{summary[STATUS_SUMMARY_KEY[s.chave]]}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </Card>
            )}

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
    </div>
  );
}
