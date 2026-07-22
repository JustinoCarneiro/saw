import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { MentoradoAdmin, MetaAdmin } from '../../shared/lib/types';
import styles from './MentoradosListaPage.module.css';

const COLUMNS = '1.4fr 1.6fr 1fr 1fr 1fr 1.8fr';

function statusPill(status: MetaAdmin['status']): { label: string; bg: string; color: string } {
  if (status === 'CONCLUIDA') return { label: 'Concluída', bg: 'var(--success-bg)', color: 'var(--success)' };
  if (status === 'PAUSADA') return { label: 'Pausada', bg: 'var(--line)', color: 'var(--text-soft)' };
  return { label: 'Ativa', bg: 'var(--info-bg)', color: 'var(--info)' };
}

// Achado de UX (22/07/2026, pedido do Marcos) — mesmo gap já corrigido em TarefasAdminPage: a
// tela só tinha listagem + import/export CSV, sem forma de criar ou editar uma meta direto, nem
// avançar status (Concluir/Pausar/Reativar). Diferente de Encaminhamento, Meta nunca nasce de um
// fluxo automático (não vem de ata) — então também precisa de criação, não só edição.
export function MetasAdminPage() {
  const [metas, setMetas] = useState<MetaAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [processandoId, setProcessandoId] = useState<string | null>(null);

  const carregar = () => {
    setMetas(null);
    apiClient
      .get<MetaAdmin[]>('/admin/metas')
      .then((res) => setMetas(res.data))
      .catch(() => setError('Não foi possível carregar as metas.'));
  };

  useEffect(carregar, []);

  async function avancarStatus(id: string, novoStatus: 'CONCLUIDA' | 'PAUSADA' | 'ATIVA') {
    setProcessandoId(id);
    setError(null);
    try {
      await apiClient.patch(`/admin/metas/${id}/status`, { novoStatus });
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
        <button className={styles.newButton} onClick={() => setCriando(true)}>
          <span style={{ fontSize: 16 }}>+</span>Nova meta
        </button>
        <CsvImportExport
          exportUrl="/admin/metas/export"
          exportFilename="metas.csv"
          importUrl="/admin/metas/import"
          onImportado={carregar}
          labelPrefix="Metas"
        />
      </div>

      {criando && (
        <NovaMetaForm onCriado={() => { setCriando(false); carregar(); }} onCancelar={() => setCriando(false)} />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Mentorado', 'Título', 'Prazo', 'Progresso', 'Status', 'Ações']}>
        {metas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {metas?.length === 0 && <div className={styles.loading}>Nenhuma meta cadastrada.</div>}
        {metas?.map((m) => (
          editandoId === m.id ? (
            <MetaEditRow
              key={m.id}
              meta={m}
              onSalvo={() => { setEditandoId(null); carregar(); }}
              onCancelar={() => setEditandoId(null)}
            />
          ) : (
            <MetaRow
              key={m.id}
              meta={m}
              processando={processandoId === m.id}
              onEditar={() => setEditandoId(m.id)}
              onAvancarStatus={(novoStatus) => avancarStatus(m.id, novoStatus)}
            />
          )
        ))}
      </DataGrid>
    </div>
  );
}

function MetaRow({ meta, processando, onEditar, onAvancarStatus }: {
  meta: MetaAdmin;
  processando: boolean;
  onEditar: () => void;
  onAvancarStatus: (novoStatus: 'CONCLUIDA' | 'PAUSADA' | 'ATIVA') => void;
}) {
  const st = statusPill(meta.status);
  return (
    <DataGridRow columns={COLUMNS} testId={`meta-admin-row-${meta.id}`}>
      <div className={styles.strong}>{meta.mentoradoNome}</div>
      <div className={styles.muted}>{meta.titulo}</div>
      <div className={styles.muted}>{new Date(meta.prazo + 'T00:00:00').toLocaleDateString('pt-BR')}</div>
      <div className={styles.muted}>{meta.progressoPct}%</div>
      <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {meta.status !== 'CONCLUIDA' && (
          <button className={styles.actionButton} disabled={processando} onClick={onEditar}>Editar</button>
        )}
        {meta.status === 'ATIVA' && (
          <>
            <button className={styles.actionButton} disabled={processando} onClick={() => onAvancarStatus('CONCLUIDA')}>
              Concluir
            </button>
            <button className={styles.actionButtonDanger} disabled={processando} onClick={() => onAvancarStatus('PAUSADA')}>
              Pausar
            </button>
          </>
        )}
        {meta.status === 'PAUSADA' && (
          <button className={styles.actionButton} disabled={processando} onClick={() => onAvancarStatus('ATIVA')}>
            Reativar
          </button>
        )}
      </div>
    </DataGridRow>
  );
}

function MetaEditRow({ meta, onSalvo, onCancelar }: {
  meta: MetaAdmin;
  onSalvo: () => void;
  onCancelar: () => void;
}) {
  const [titulo, setTitulo] = useState(meta.titulo);
  const [prazo, setPrazo] = useState(meta.prazo);
  const [progressoPct, setProgressoPct] = useState(meta.progressoPct);
  const [error, setError] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function salvar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSalvando(true);
    try {
      // descricao preservada como estava — a tela admin não expõe esse campo pra edição.
      await apiClient.put(`/admin/metas/${meta.id}`, { titulo, descricao: meta.descricao, prazo, progressoPct });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar a meta.'));
      setSalvando(false);
    }
  }

  return (
    <DataGridRow columns={COLUMNS} testId={`meta-admin-editando-${meta.id}`}>
      <div className={styles.strong}>{meta.mentoradoNome}</div>
      <input
        className={styles.textInput}
        value={titulo}
        onChange={(e) => setTitulo(e.target.value)}
        data-testid={`meta-admin-titulo-${meta.id}`}
      />
      <input
        className={styles.textInput}
        type="date"
        aria-label={`Prazo de ${meta.titulo}`}
        value={prazo}
        onChange={(e) => setPrazo(e.target.value)}
      />
      <input
        className={styles.textInput}
        type="number"
        min={0}
        max={100}
        aria-label={`Progresso de ${meta.titulo}`}
        value={progressoPct}
        onChange={(e) => setProgressoPct(Number(e.target.value))}
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

function NovaMetaForm({ onCriado, onCancelar }: { onCriado: () => void; onCancelar: () => void }) {
  const [mentorados, setMentorados] = useState<MentoradoAdmin[]>([]);
  const [mentoradoId, setMentoradoId] = useState('');
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [prazo, setPrazo] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiClient.get<MentoradoAdmin[]>('/admin/mentorados').then((res) => setMentorados(res.data));
  }, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!mentoradoId) {
      setError('Selecione um mentorado.');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post('/admin/metas', { mentoradoId, titulo, descricao: descricao || null, prazo });
      onCriado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar a meta.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Nova meta</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Mentorado
            <select className={styles.select} value={mentoradoId} onChange={(e) => setMentoradoId(e.target.value)} required>
              <option value="">{mentorados.length === 0 ? 'Carregando…' : 'Selecione…'}</option>
              {mentorados.map((m) => (
                <option key={m.id} value={m.id}>{m.nome}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField} style={{ flex: 2 }}>
            Título
            <input className={styles.textInput} value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Prazo
            <input className={styles.textInput} type="date" value={prazo} onChange={(e) => setPrazo(e.target.value)} required />
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 1 }}>
            Descrição (opcional)
            <input className={styles.textInput} value={descricao} onChange={(e) => setDescricao(e.target.value)} />
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Criar meta'}
          </button>
        </div>
      </form>
    </Card>
  );
}
