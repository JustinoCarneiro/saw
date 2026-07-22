import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import type { DashboardFaturamentoResponse, OrigemReceita } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import { Tooltip } from '../../shared/components/Tooltip';
import styles from './DashboardFaturamentoPage.module.css';

const ORIGEM_LABEL: Record<OrigemReceita, string> = {
  ASSINATURA: 'Assinaturas (recorrência)',
  LOJA: 'Loja SAW',
  EVENTO: 'Eventos',
  OUTRA: 'Outras receitas',
};

const ORIGEM_COLOR: Record<OrigemReceita, string> = {
  ASSINATURA: 'var(--gold)',
  LOJA: 'var(--info)',
  EVENTO: 'var(--success)',
  OUTRA: 'var(--text-faint)',
};

export function DashboardFaturamentoPage() {
  const now = new Date();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [dashboard, setDashboard] = useState<DashboardFaturamentoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDashboard(null);
    apiClient
      .get<DashboardFaturamentoResponse>('/admin/financeiro/dashboard-faturamento', { params: { ano, mes } })
      .then((res) => setDashboard(res.data))
      .catch(() => setError('Não foi possível carregar o dashboard de faturamento.'));
  }, [ano, mes]);

  const total = dashboard?.composicao.reduce((acc, c) => acc + c.valor, 0) ?? 0;

  return (
    <div>
      <div className={styles.toolbar}>
        <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {dashboard && (
        <>
          <div className={styles.kpis}>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Soma de todos os lançamentos de receita Realizados no mês/ano selecionado.">Faturamento do mês</Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.faturamentoMensal)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Receita mensal recorrente — contratos de mentoria em andamento, sem contar receitas pontuais (Loja, eventos).">Receita recorrente (MRR)</Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.mrr)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="% de queda do MRR em relação ao mês anterior.">Churn de receita</Tooltip>
              </div>
              <div className={styles.kpiValue} style={{ color: dashboard.churnPct > 0 ? 'var(--danger)' : 'var(--success)' }}>
                {dashboard.churnPct.toFixed(1)}%
              </div>
              <div className={styles.kpiHint}>{dashboard.churnPct > 0 ? 'queda de MRR vs. mês anterior' : 'sem queda de MRR'}</div>
            </Card>
          </div>

          <Card style={{ padding: '20px 22px' }}>
            <div className={styles.sectionTitle}>
              <Tooltip text="Como a receita realizada do período se divide por origem (assinaturas, Loja, eventos, outras).">Composição da receita</Tooltip>
            </div>
            <div className={styles.composicaoList}>
              {dashboard.composicao.length === 0 && (
                <div className={styles.empty}>Sem receitas realizadas neste período.</div>
              )}
              {dashboard.composicao.map((c) => {
                const pct = total === 0 ? 0 : (c.valor / total) * 100;
                return (
                  <div key={c.origem} className={styles.composicaoRow}>
                    <div className={styles.composicaoHeader}>
                      <span className={styles.composicaoLabel}>
                        <span className={styles.dot} style={{ background: ORIGEM_COLOR[c.origem] }} />
                        {ORIGEM_LABEL[c.origem]}
                      </span>
                      <span className={styles.composicaoValue}>
                        {formatBRL(c.valor)} <span className={styles.composicaoPct}>({pct.toFixed(0)}%)</span>
                      </span>
                    </div>
                    <div className={styles.track}>
                      <div className={styles.fill} style={{ width: `${pct}%`, background: ORIGEM_COLOR[c.origem] }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </Card>
        </>
      )}
    </div>
  );
}
