import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import type { MetaAdmin } from '../../shared/lib/types';
import styles from './MentoradosListaPage.module.css';

const COLUMNS = '1.6fr 2fr 1fr 1fr 1fr';

function statusPill(status: MetaAdmin['status']): { label: string; bg: string; color: string } {
  if (status === 'CONCLUIDA') return { label: 'Concluída', bg: 'var(--success-bg)', color: 'var(--success)' };
  if (status === 'PAUSADA') return { label: 'Pausada', bg: 'var(--line)', color: 'var(--text-soft)' };
  return { label: 'Ativa', bg: 'var(--info-bg)', color: 'var(--info)' };
}

export function MetasAdminPage() {
  const [metas, setMetas] = useState<MetaAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const carregar = () => {
    setMetas(null);
    apiClient
      .get<MetaAdmin[]>('/admin/metas')
      .then((res) => setMetas(res.data))
      .catch(() => setError('Não foi possível carregar as metas.'));
  };

  useEffect(carregar, []);

  return (
    <div>
      <div className={styles.csvRow}>
        <CsvImportExport
          exportUrl="/admin/metas/export"
          exportFilename="metas.csv"
          importUrl="/admin/metas/import"
          onImportado={carregar}
          labelPrefix="Metas"
        />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Mentorado', 'Título', 'Prazo', 'Progresso', 'Status']}>
        {metas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {metas?.length === 0 && <div className={styles.loading}>Nenhuma meta cadastrada.</div>}
        {metas?.map((m) => {
          const st = statusPill(m.status);
          return (
            <DataGridRow key={m.id} columns={COLUMNS}>
              <div className={styles.strong}>{m.mentoradoNome}</div>
              <div className={styles.muted}>{m.titulo}</div>
              <div className={styles.muted}>{new Date(m.prazo + 'T00:00:00').toLocaleDateString('pt-BR')}</div>
              <div className={styles.muted}>{m.progressoPct}%</div>
              <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
            </DataGridRow>
          );
        })}
      </DataGrid>
    </div>
  );
}
