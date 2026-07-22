import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DonutChart } from '../../shared/components/DonutChart';
import { Pill } from '../../shared/components/Pill';
import { ProgressBar } from '../../shared/components/ProgressBar';
import { Tooltip } from '../../shared/components/Tooltip';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { CATEGORIA_COR, CATEGORIA_ICONE } from '../../shared/lib/avisoDisplay';
import { formatarQuando } from '../../shared/lib/format';
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
        <Card style={{ padding: 18 }} testId="kpi-tarefas-abertas">
          <div className={styles.kpiLabel}>
            <Tooltip text="Suas tarefas com status Pendente ou Em andamento agora.">Tarefas abertas</Tooltip>
          </div>
          <div className={styles.kpiValue}>{dashboard.tarefasAbertas}</div>
        </Card>
        <Card style={{ padding: 18 }}>
          <div className={styles.kpiLabel}>
            <Tooltip text="Progresso da sua meta ativa com prazo mais próximo.">Meta semanal</Tooltip>
          </div>
          {dashboard.metaSemanalPct === null ? (
            <div className={styles.kpiHint}>Ainda não disponível</div>
          ) : (
            <>
              <div className={styles.kpiValue}>{dashboard.metaSemanalPct}%</div>
              <ProgressBar pct={dashboard.metaSemanalPct} />
            </>
          )}
        </Card>
        <Card style={{ padding: 18, display: 'flex', alignItems: 'center', gap: 14 }}>
          <DonutChart
            titulo="Evolução geral"
            tamanho={88}
            segmentos={[
              { chave: 'evolucao', valor: dashboard.evolucaoGeralPct, cor: 'var(--gold)' },
              { chave: 'resto', valor: 100 - dashboard.evolucaoGeralPct, cor: 'var(--elevated)' },
            ]}
            centroConteudo={<span className={styles.evolucaoRingLabel}>{dashboard.evolucaoGeralPct}%</span>}
          />
          <div className={styles.kpiLabel}>
            <Tooltip text="Combina o progresso das suas metas e tarefas numa única métrica geral.">Evolução geral</Tooltip>
          </div>
        </Card>
      </div>

      <div className={styles.grid}>
        <Card style={{ padding: 20 }}>
          <div className={styles.sectionTitle}>
            <Tooltip text="Sua próxima mentoria Agendada ou Confirmada, individual ou em grupo.">Próxima reunião</Tooltip>
          </div>
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
          <div className={styles.sectionTitle}>
            <Tooltip text="Conteúdo em destaque da biblioteca de materiais, selecionado pela SAW.">Dica do Brayan</Tooltip>
          </div>
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
        <div className={styles.sectionTitle}>
          <Tooltip text="Suas próximas mentorias agendadas, em ordem cronológica.">Compromissos</Tooltip>
        </div>
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

      <Card style={{ padding: 20, marginTop: 16 }} testId="avisos-importantes">
        <div className={styles.sectionTitle}>
          <Tooltip text="Comunicados publicados pela SAW pra todos os mentorados ativos.">Avisos importantes</Tooltip>
        </div>
        {dashboard.avisos.length === 0 ? (
          <div className={styles.kpiHint}>Nenhum aviso no momento.</div>
        ) : (
          <ul className={styles.avisosList}>
            {dashboard.avisos.map((aviso) => (
              <li key={aviso.id} className={styles.avisoItem}>
                <span className={styles.avisoIcone} style={{ background: CATEGORIA_COR[aviso.categoria].bg }}>
                  {CATEGORIA_ICONE[aviso.categoria]}
                </span>
                <div className={styles.avisoTexto}>
                  <div className={styles.avisoTitulo}>{aviso.titulo}</div>
                  <div className={styles.avisoDescricao}>{aviso.descricao}</div>
                </div>
                <span className={styles.avisoQuando}>{formatarQuando(aviso.quando)}</span>
                {!aviso.lido && <span className={styles.avisoDot} />}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
