import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { EncaminhamentoAdmin, Prioridade } from '../../shared/lib/types';
import styles from './MentoradosListaPage.module.css';

const COLUMNS = '1.4fr 1.6fr .8fr 1fr 1fr 1fr 1.8fr';

const PRIORIDADE_LABEL: Record<Prioridade, string> = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };

function statusPill(status: EncaminhamentoAdmin['status']): { label: string; bg: string; color: string } {
  if (status === 'CONCLUIDA') return { label: 'Concluída', bg: 'var(--success-bg)', color: 'var(--success)' };
  if (status === 'EM_ANDAMENTO') return { label: 'Em andamento', bg: 'var(--info-bg)', color: 'var(--info)' };
  return { label: 'Pendente', bg: 'var(--line)', color: 'var(--text-soft)' };
}

// Achado de UX (22/07/2026, pedido do Marcos) — a tela só tinha listagem + import/export CSV, sem
// forma de editar título/prazo/prioridade nem avançar status de um encaminhamento existente.
// Editar direto na linha (mesmo padrão de SugestaoRow em AtaDetalhePage), sem modal separado —
// peso nunca é editável aqui (só o construtor self-service/ata define peso).
export function TarefasAdminPage() {
  const [tarefas, setTarefas] = useState<EncaminhamentoAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [processandoId, setProcessandoId] = useState<string | null>(null);

  const carregar = () => {
    setTarefas(null);
    apiClient
      .get<EncaminhamentoAdmin[]>('/admin/encaminhamentos')
      .then((res) => setTarefas(res.data))
      .catch(() => setError('Não foi possível carregar os encaminhamentos.'));
  };

  useEffect(carregar, []);

  async function avancarStatus(id: string, novoStatus: 'EM_ANDAMENTO' | 'CONCLUIDA' | 'PENDENTE') {
    setProcessandoId(id);
    setError(null);
    try {
      await apiClient.patch(`/admin/encaminhamentos/${id}/status`, { novoStatus });
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível concluir a ação.'));
    } finally {
      setProcessandoId(null);
    }
  }

  return (
    <div>
      <div className={styles.csvRow}>
        <CsvImportExport
          exportUrl="/admin/encaminhamentos/export"
          exportFilename="encaminhamentos.csv"
          importUrl="/admin/encaminhamentos/import"
          onImportado={carregar}
          labelPrefix="Encaminhamentos"
        />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Mentorado', 'Título', 'Peso', 'Prioridade', 'Prazo', 'Status', 'Ações']}>
        {tarefas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {tarefas?.length === 0 && <div className={styles.loading}>Nenhum encaminhamento cadastrado.</div>}
        {tarefas?.map((t) => (
          editandoId === t.id ? (
            <TarefaEditRow
              key={t.id}
              tarefa={t}
              onSalvo={() => { setEditandoId(null); carregar(); }}
              onCancelar={() => setEditandoId(null)}
            />
          ) : (
            <TarefaRow
              key={t.id}
              tarefa={t}
              processando={processandoId === t.id}
              onEditar={() => setEditandoId(t.id)}
              onAvancarStatus={(novoStatus) => avancarStatus(t.id, novoStatus)}
            />
          )
        ))}
      </DataGrid>
    </div>
  );
}

function TarefaRow({ tarefa, processando, onEditar, onAvancarStatus }: {
  tarefa: EncaminhamentoAdmin;
  processando: boolean;
  onEditar: () => void;
  onAvancarStatus: (novoStatus: 'EM_ANDAMENTO' | 'CONCLUIDA' | 'PENDENTE') => void;
}) {
  const st = statusPill(tarefa.status);
  return (
    <DataGridRow columns={COLUMNS} testId={`tarefa-admin-row-${tarefa.id}`}>
      <div className={styles.strong}>{tarefa.mentoradoNome}</div>
      <div className={styles.muted}>{tarefa.titulo}</div>
      <div className={styles.muted}>{tarefa.peso}</div>
      <div className={styles.muted}>{PRIORIDADE_LABEL[tarefa.prioridade]}</div>
      <div className={styles.muted}>{tarefa.prazo ? new Date(tarefa.prazo + 'T00:00:00').toLocaleDateString('pt-BR') : 'Sem prazo'}</div>
      <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {tarefa.status !== 'CONCLUIDA' && (
          <button className={styles.actionButton} disabled={processando} onClick={onEditar}>Editar</button>
        )}
        {tarefa.status === 'PENDENTE' && (
          <button className={styles.actionButton} disabled={processando} onClick={() => onAvancarStatus('EM_ANDAMENTO')}>
            Iniciar
          </button>
        )}
        {tarefa.status !== 'CONCLUIDA' && (
          <button className={styles.actionButton} disabled={processando} onClick={() => onAvancarStatus('CONCLUIDA')}>
            Concluir
          </button>
        )}
        {tarefa.status === 'CONCLUIDA' && (
          <button className={styles.actionButtonDanger} disabled={processando} onClick={() => onAvancarStatus('PENDENTE')}>
            Reabrir
          </button>
        )}
      </div>
    </DataGridRow>
  );
}

function TarefaEditRow({ tarefa, onSalvo, onCancelar }: {
  tarefa: EncaminhamentoAdmin;
  onSalvo: () => void;
  onCancelar: () => void;
}) {
  const [titulo, setTitulo] = useState(tarefa.titulo);
  const [prazo, setPrazo] = useState(tarefa.prazo ?? '');
  const [prioridade, setPrioridade] = useState<Prioridade>(tarefa.prioridade);
  const [error, setError] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function salvar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSalvando(true);
    try {
      await apiClient.put(`/admin/encaminhamentos/${tarefa.id}`, { titulo, prazo: prazo || null, prioridade });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o encaminhamento.'));
      setSalvando(false);
    }
  }

  return (
    <DataGridRow columns={COLUMNS} testId={`tarefa-admin-editando-${tarefa.id}`}>
      <div className={styles.strong}>{tarefa.mentoradoNome}</div>
      <input
        className={styles.textInput}
        value={titulo}
        onChange={(e) => setTitulo(e.target.value)}
        data-testid={`tarefa-admin-titulo-${tarefa.id}`}
      />
      <div className={styles.muted}>{tarefa.peso}</div>
      <select className={styles.select} value={prioridade} onChange={(e) => setPrioridade(e.target.value as Prioridade)}>
        <option value="ALTA">Alta</option>
        <option value="MEDIA">Média</option>
        <option value="BAIXA">Baixa</option>
      </select>
      <input
        className={styles.textInput}
        type="date"
        aria-label={`Prazo de ${tarefa.titulo}`}
        value={prazo}
        onChange={(e) => setPrazo(e.target.value)}
      />
      <div />
      <div style={{ display: 'flex', gap: 6, flexDirection: 'column' }}>
        <div style={{ display: 'flex', gap: 6 }}>
          <button className={styles.actionButton} disabled={salvando || !titulo.trim()} onClick={salvar}>
            {salvando ? 'Salvando…' : 'Salvar'}
          </button>
          <button type="button" className={styles.actionButtonDanger} disabled={salvando} onClick={onCancelar}>
            Cancelar
          </button>
        </div>
        {error && <div className={styles.error}>{error}</div>}
      </div>
    </DataGridRow>
  );
}
