import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import type { EncaminhamentoAdmin, Prioridade } from '../../shared/lib/types';
import styles from './MentoradosListaPage.module.css';

const COLUMNS = '1.4fr 1.8fr .8fr 1fr 1fr 1fr';

const PRIORIDADE_LABEL: Record<Prioridade, string> = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };

function statusPill(status: EncaminhamentoAdmin['status']): { label: string; bg: string; color: string } {
  if (status === 'CONCLUIDA') return { label: 'Concluída', bg: 'var(--success-bg)', color: 'var(--success)' };
  if (status === 'EM_ANDAMENTO') return { label: 'Em andamento', bg: 'var(--info-bg)', color: 'var(--info)' };
  return { label: 'Pendente', bg: 'var(--line)', color: 'var(--text-soft)' };
}

export function TarefasAdminPage() {
  const [tarefas, setTarefas] = useState<EncaminhamentoAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const carregar = () => {
    setTarefas(null);
    apiClient
      .get<EncaminhamentoAdmin[]>('/admin/encaminhamentos')
      .then((res) => setTarefas(res.data))
      .catch(() => setError('Não foi possível carregar as tarefas.'));
  };

  useEffect(carregar, []);

  return (
    <div>
      <div className={styles.csvRow}>
        <CsvImportExport
          exportUrl="/admin/encaminhamentos/export"
          exportFilename="tarefas.csv"
          importUrl="/admin/encaminhamentos/import"
          onImportado={carregar}
          labelPrefix="Tarefas"
        />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Mentorado', 'Título', 'Peso', 'Prioridade', 'Prazo', 'Status']}>
        {tarefas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {tarefas?.length === 0 && <div className={styles.loading}>Nenhuma tarefa cadastrada.</div>}
        {tarefas?.map((t) => {
          const st = statusPill(t.status);
          return (
            <DataGridRow key={t.id} columns={COLUMNS}>
              <div className={styles.strong}>{t.mentoradoNome}</div>
              <div className={styles.muted}>{t.titulo}</div>
              <div className={styles.muted}>{t.peso}</div>
              <div className={styles.muted}>{PRIORIDADE_LABEL[t.prioridade]}</div>
              <div className={styles.muted}>{t.prazo ? new Date(t.prazo + 'T00:00:00').toLocaleDateString('pt-BR') : '—'}</div>
              <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
            </DataGridRow>
          );
        })}
      </DataGrid>
    </div>
  );
}
