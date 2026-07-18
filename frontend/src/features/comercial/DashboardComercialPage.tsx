import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import type { DashboardComercialResponse, StatusLead } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import styles from './DashboardComercialPage.module.css';

const STATUS_LABEL: Record<StatusLead, string> = {
  SOLICITACAO: 'Solicitação',
  EM_CONTATO: 'Em contato',
  DIAGNOSTICO: 'Diagnóstico',
  PROPOSTA: 'Proposta',
  FECHADO: 'Fechado',
  PERDIDO: 'Perdido',
};

const STATUS_COLOR: Record<StatusLead, string> = {
  SOLICITACAO: 'var(--text-faint)',
  EM_CONTATO: 'var(--info)',
  DIAGNOSTICO: 'var(--info)',
  PROPOSTA: 'var(--gold)',
  FECHADO: 'var(--success)',
  PERDIDO: 'var(--danger)',
};

export function DashboardComercialPage() {
  const now = new Date();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [dashboard, setDashboard] = useState<DashboardComercialResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDashboard(null);
    apiClient
      .get<DashboardComercialResponse>('/admin/comercial/dashboard', { params: { ano, mes } })
      .then((res) => setDashboard(res.data))
      .catch(() => setError('Não foi possível carregar o dashboard comercial.'));
  }, [ano, mes]);

  const maiorQuantidade = Math.max(1, ...(dashboard?.funil.map((f) => f.quantidade) ?? [1]));

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
              <div className={styles.kpiLabel}>Novos mentorados no mês</div>
              <div className={styles.kpiValue}>{dashboard.novosMentoradosNoMes}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>Taxa de conversão</div>
              <div className={styles.kpiValue}>{dashboard.taxaConversaoPct.toFixed(1)}%</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>Receita recorrente (MRR)</div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.mrr)}</div>
              <div className={styles.kpiHint} style={{ color: dashboard.variacaoMrrPct >= 0 ? 'var(--success)' : 'var(--danger)' }}>
                {dashboard.variacaoMrrPct >= 0 ? '+' : ''}{dashboard.variacaoMrrPct.toFixed(1)}% vs. mês anterior
              </div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>Vendas da loja</div>
              <div className={styles.kpiValue}>{formatBRL(dashboard.vendasLoja)}</div>
            </Card>
          </div>

          <Card style={{ padding: '20px 22px' }}>
            <div className={styles.sectionTitle}>Funil de vendas</div>
            <div className={styles.funilList}>
              {dashboard.funil.map((f) => {
                const pct = (f.quantidade / maiorQuantidade) * 100;
                return (
                  <div key={f.status} className={styles.funilRow}>
                    <div className={styles.funilHeader}>
                      <span className={styles.funilLabel}>
                        <span className={styles.dot} style={{ background: STATUS_COLOR[f.status] }} />
                        {STATUS_LABEL[f.status]}
                      </span>
                      <span className={styles.funilValue}>{f.quantidade}</span>
                    </div>
                    <div className={styles.track}>
                      <div className={styles.fill} style={{ width: `${pct}%`, background: STATUS_COLOR[f.status] }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </Card>

          {dashboard.vendaIngressos.length > 0 && (
            <Card style={{ padding: '20px 22px', marginTop: 16 }}>
              <div className={styles.sectionTitle}>Venda de ingressos por evento</div>
              <div className={styles.funilList}>
                {dashboard.vendaIngressos.map((v) => (
                  <div key={v.eventoId} className={styles.funilRow}>
                    <div className={styles.funilHeader}>
                      <span className={styles.funilLabel}>{v.eventoTitulo}</span>
                      <span className={styles.funilValue}>
                        {v.quantidadeVendida}{v.quantidadeTotal != null ? ` / ${v.quantidadeTotal}` : ''} — {formatBRL(v.valorLiquido)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
