import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import type { RankingComercialItem } from '../../shared/lib/types';
import styles from './RankingComercialPage.module.css';

const COLUMNS = '1.6fr 1fr 1fr 2fr';

export function RankingComercialPage() {
  const now = new Date();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [ranking, setRanking] = useState<RankingComercialItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setRanking(null);
    apiClient
      .get<RankingComercialItem[]>('/admin/comercial/ranking', { params: { ano, mes } })
      .then((res) => setRanking(res.data))
      .catch(() => setError('Não foi possível carregar o ranking.'));
  }, [ano, mes]);

  return (
    <div>
      <div className={styles.toolbar}>
        <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Vendedor', 'Meta', 'Realizado', '% atingido']}>
        {ranking === null && !error && <div className={styles.loading}>Carregando…</div>}
        {ranking?.length === 0 && <div className={styles.loading}>Nenhuma meta cadastrada para o período.</div>}
        {ranking?.map((item, i) => {
          const pct = Math.min(100, item.pctAtingido);
          const cor = item.pctAtingido >= 100 ? 'var(--success)' : item.pctAtingido >= 60 ? 'var(--gold)' : 'var(--danger)';
          return (
            <DataGridRow key={item.vendedor.id} columns={COLUMNS}>
              <div className={styles.vendedor}>
                <span className={styles.pos}>#{i + 1}</span>
                {item.vendedor.nome}
              </div>
              <div className={styles.muted}>{item.metaFechamentos}</div>
              <div className={styles.strong}>{item.realizado}</div>
              <div className={styles.progressoCell}>
                <div className={styles.track}>
                  <div className={styles.fill} style={{ width: `${pct}%`, background: cor }} />
                </div>
                <span className={styles.pctValue} style={{ color: cor }}>{item.pctAtingido.toFixed(1)}%</span>
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>
    </div>
  );
}
