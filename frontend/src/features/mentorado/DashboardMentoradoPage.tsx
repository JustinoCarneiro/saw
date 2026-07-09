import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { CompromissoMentorado, DashboardMentoradoResponse } from '../../shared/lib/types';
import styles from './DashboardMentoradoPage.module.css';

function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

const TIPO_LABEL: Record<CompromissoMentorado['tipo'], string> = {
  INDIVIDUAL: 'Mentoria individual',
  GRUPO: 'Mentoria em grupo',
};

export function DashboardMentoradoPage() {
  const [dashboard, setDashboard] = useState<DashboardMentoradoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient
      .get<DashboardMentoradoResponse>('/mentorado/dashboard')
      .then((res) => setDashboard(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar seu Dashboard agora.')));
  }, []);

  if (error) {
    return <div className={styles.error}>{error}</div>;
  }

  if (!dashboard) {
    return <div style={{ color: 'var(--text-soft)' }}>Carregando…</div>;
  }

  return (
    <div>
      <h1 className={styles.title}>Olá, {dashboard.nome.split(' ')[0]}</h1>
      <p className={styles.subtitle}>Aqui está o resumo da sua jornada.</p>

      <div className={styles.kpis}>
        <Card style={{ padding: 18 }}>
          <div className={styles.kpiLabel}>Evolução geral</div>
          <div className={styles.kpiValue}>{dashboard.evolucaoGeralPct}%</div>
        </Card>
        <Card style={{ padding: 18 }}>
          <div className={styles.kpiLabel}>Tarefas abertas</div>
          <div className={styles.kpiValue}>{dashboard.tarefasAbertas}</div>
        </Card>
        <Card style={{ padding: 18 }}>
          <div className={styles.kpiLabel}>Meta semanal</div>
          {dashboard.metaSemanalPct === null ? (
            <div className={styles.kpiHint}>Ainda não disponível</div>
          ) : (
            <div className={styles.kpiValue}>{dashboard.metaSemanalPct}%</div>
          )}
        </Card>
      </div>

      <div className={styles.grid}>
        <Card style={{ padding: 20 }}>
          <div className={styles.sectionTitle}>Próxima reunião</div>
          {dashboard.proximaReuniao ? (
            <div className={styles.proximaReuniao}>
              <Pill bg="var(--info-bg)" color="var(--info)">{TIPO_LABEL[dashboard.proximaReuniao.tipo]}</Pill>
              <div className={styles.proximaReuniaoData}>{formatarDataHora(dashboard.proximaReuniao.dataHora)}</div>
              {dashboard.proximaReuniao.linkOnline && (
                <a className={styles.link} href={dashboard.proximaReuniao.linkOnline} target="_blank" rel="noreferrer">
                  Entrar na reunião
                </a>
              )}
              {dashboard.proximaReuniao.local && <div className={styles.kpiHint}>{dashboard.proximaReuniao.local}</div>}
            </div>
          ) : (
            <div className={styles.kpiHint}>Nenhuma reunião agendada.</div>
          )}
        </Card>

        <Card style={{ padding: 20 }}>
          <div className={styles.sectionTitle}>Dica do Brayan</div>
          {dashboard.dicaDestaque ? (
            <a className={styles.dicaCard} href={dashboard.dicaDestaque.url} target="_blank" rel="noreferrer">
              {dashboard.dicaDestaque.titulo}
            </a>
          ) : (
            <div className={styles.kpiHint}>Nenhuma dica disponível no momento.</div>
          )}
        </Card>
      </div>

      <Card style={{ padding: 20, marginTop: 16 }}>
        <div className={styles.sectionTitle}>Compromissos</div>
        {dashboard.compromissos.length === 0 ? (
          <div className={styles.kpiHint}>Nenhum compromisso futuro.</div>
        ) : (
          <div className={styles.compromissosList}>
            {dashboard.compromissos.map((c) => (
              <div key={c.id} className={styles.compromissoRow}>
                <Pill bg="var(--info-bg)" color="var(--info)">{TIPO_LABEL[c.tipo]}</Pill>
                <span>{formatarDataHora(c.dataHora)}</span>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card style={{ padding: 20, marginTop: 16 }}>
        <div className={styles.sectionTitle}>Avisos</div>
        {dashboard.avisos.length === 0 ? (
          <div className={styles.kpiHint}>Nenhum aviso no momento.</div>
        ) : (
          <ul>
            {dashboard.avisos.map((aviso) => (
              <li key={aviso}>{aviso}</li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
