import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { ProgressBar } from '../../shared/components/ProgressBar';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Meta, ResumoMetas, StatusMeta } from '../../shared/lib/types';
import styles from './MetasPage.module.css';

const COLUMNS = '2fr 1.3fr 1.1fr 1fr 1.6fr';

const FILTROS: { label: string; status: StatusMeta | '' }[] = [
  { label: 'Ativas', status: 'ATIVA' },
  { label: 'Concluídas', status: 'CONCLUIDA' },
  { label: 'Pausadas', status: 'PAUSADA' },
  { label: 'Todas', status: '' },
];

function statusPill(meta: Meta): { label: string; bg: string; color: string } {
  if (meta.status === 'CONCLUIDA') return { label: 'Concluída', bg: 'var(--success-bg)', color: 'var(--success)' };
  if (meta.status === 'PAUSADA') return { label: 'Pausada', bg: 'var(--line)', color: 'var(--text-soft)' };
  if (meta.subStatus === 'ATRASADA') return { label: 'Atrasada', bg: 'var(--danger-bg)', color: 'var(--danger)' };
  if (meta.subStatus === 'ATENCAO') return { label: 'Atenção', bg: 'var(--warning-bg)', color: 'var(--warning)' };
  return { label: 'No prazo', bg: 'var(--success-bg)', color: 'var(--success)' };
}

function formatarPrazo(iso: string): string {
  return new Date(iso + 'T00:00:00').toLocaleDateString('pt-BR');
}

function diasRestantesLabel(dias: number): string {
  if (dias < 0) return `${Math.abs(dias)} dia(s) atrasada`;
  if (dias === 0) return 'Vence hoje';
  return `${dias} dia(s) restantes`;
}

export function MetasPage() {
  const [filtro, setFiltro] = useState<StatusMeta | ''>('ATIVA');
  const [metas, setMetas] = useState<Meta[] | null>(null);
  const [resumo, setResumo] = useState<ResumoMetas | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formulario, setFormulario] = useState<'fechado' | 'criar' | Meta>('fechado');
  const [processando, setProcessando] = useState<string | null>(null);

  const carregar = () => {
    setMetas(null);
    apiClient
      .get<Meta[]>('/mentorado/metas', { params: { status: filtro || undefined } })
      .then((res) => setMetas(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar suas metas.')));
    apiClient.get<ResumoMetas>('/mentorado/metas/resumo').then((res) => setResumo(res.data));
  };

  useEffect(carregar, [filtro]);

  async function avancarStatus(id: string, novoStatus: 'CONCLUIDA' | 'PAUSADA' | 'ATIVA') {
    setProcessando(id);
    try {
      await apiClient.patch(`/mentorado/metas/${id}/status`, { novoStatus });
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível concluir a ação.'));
    } finally {
      setProcessando(null);
    }
  }

  return (
    <div>
      <h1 className={styles.title}>Metas estratégicas</h1>
      <p className={styles.subtitle}>Acompanhe suas metas, prazos e evolução.</p>

      <div className={styles.toolbar}>
        <div className={styles.tabs}>
          {FILTROS.map((f) => (
            <button
              key={f.label}
              className={`${styles.tab} ${filtro === f.status ? styles.tabActive : ''}`}
              onClick={() => setFiltro(f.status)}
            >
              {f.label}
            </button>
          ))}
        </div>
        <button className={styles.newButton} onClick={() => setFormulario('criar')}>
          <span style={{ fontSize: 16 }}>+</span>Nova meta
        </button>
      </div>

      {formulario !== 'fechado' && (
        <MetaForm
          metaExistente={formulario === 'criar' ? null : formulario}
          onSalva={() => { setFormulario('fechado'); carregar(); }}
          onCancelar={() => setFormulario('fechado')}
        />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Meta', 'Progresso', 'Prazo', 'Status', 'Ações']}>
        {metas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {metas?.length === 0 && <div className={styles.loading}>Nenhuma meta encontrada.</div>}
        {metas?.map((m) => {
          const pill = statusPill(m);
          const emProcessamento = processando === m.id;
          return (
            <DataGridRow key={m.id} columns={COLUMNS} testId={`meta-row-${m.id}`}>
              <div>
                <div className={styles.strong}>{m.titulo}</div>
                {m.descricao && <div className={styles.muted}>{m.descricao}</div>}
              </div>
              <div>
                <div className={styles.pctLabel}>{m.progressoPct}%</div>
                <ProgressBar pct={m.progressoPct} />
              </div>
              <div>
                <div className={styles.strong}>{formatarPrazo(m.prazo)}</div>
                <div className={styles.muted}>{diasRestantesLabel(m.diasRestantes)}</div>
              </div>
              <div><Pill bg={pill.bg} color={pill.color}>{pill.label}</Pill></div>
              <div className={styles.acoes}>
                {m.status !== 'CONCLUIDA' && (
                  <button className={styles.actionButton} disabled={emProcessamento} onClick={() => setFormulario(m)}>
                    Editar
                  </button>
                )}
                {m.status === 'ATIVA' && (
                  <>
                    <button className={styles.actionButton} disabled={emProcessamento} onClick={() => avancarStatus(m.id, 'CONCLUIDA')}>
                      Concluir
                    </button>
                    <button className={styles.actionButtonDanger} disabled={emProcessamento} onClick={() => avancarStatus(m.id, 'PAUSADA')}>
                      Pausar
                    </button>
                  </>
                )}
                {m.status === 'PAUSADA' && (
                  <button className={styles.actionButton} disabled={emProcessamento} onClick={() => avancarStatus(m.id, 'ATIVA')}>
                    Reativar
                  </button>
                )}
                {m.status === 'CONCLUIDA' && <span className={styles.muted}>—</span>}
              </div>
            </DataGridRow>
          );
        })}
        {resumo && (
          <DataGridRow columns={COLUMNS}>
            <div>
              <div className={styles.strong}>Resumo geral</div>
              <div className={styles.muted}>Você concluiu {resumo.concluidas} meta(s).</div>
            </div>
            <div>
              <div className={styles.summaryValue} style={{ color: 'var(--gold)' }}>{resumo.conclusaoMediaPct}%</div>
              <div className={styles.muted}>Conclusão geral</div>
            </div>
            <div>
              <div className={styles.summaryValue}>{resumo.concluidas}</div>
              <div className={styles.muted}>Concluídas</div>
            </div>
            <div>
              <div className={styles.summaryValue} style={{ color: 'var(--success)' }}>{resumo.noPrazo}</div>
              <div className={styles.muted}>No prazo</div>
            </div>
            <div>
              <div className={styles.summaryValue} style={{ color: 'var(--danger)' }}>{resumo.atrasadas}</div>
              <div className={styles.muted}>Atrasadas</div>
            </div>
          </DataGridRow>
        )}
      </DataGrid>
    </div>
  );
}

function MetaForm({ metaExistente, onSalva, onCancelar }: {
  metaExistente: Meta | null;
  onSalva: () => void;
  onCancelar: () => void;
}) {
  const [titulo, setTitulo] = useState(metaExistente?.titulo ?? '');
  const [descricao, setDescricao] = useState(metaExistente?.descricao ?? '');
  const [prazo, setPrazo] = useState(metaExistente?.prazo ?? '');
  const [progressoPct, setProgressoPct] = useState(String(metaExistente?.progressoPct ?? 0));
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      if (metaExistente) {
        await apiClient.put(`/mentorado/metas/${metaExistente.id}`, {
          titulo, descricao: descricao || null, prazo, progressoPct: Number(progressoPct),
        });
      } else {
        await apiClient.post('/mentorado/metas', { titulo, descricao: descricao || null, prazo });
      }
      onSalva();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar a meta. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>{metaExistente ? 'Editar meta' : 'Nova meta'}</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.formField}>
          Título
          <input className={styles.textInput} value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
        </label>
        <label className={styles.formField}>
          Descrição (opcional)
          <input className={styles.textInput} value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        </label>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Prazo
            <input className={styles.textInput} type="date" value={prazo} onChange={(e) => setPrazo(e.target.value)} required />
          </label>
          {metaExistente && (
            <label className={styles.formField}>
              Progresso (%)
              <input
                className={styles.textInput}
                type="number"
                min="0"
                max="100"
                value={progressoPct}
                onChange={(e) => setProgressoPct(e.target.value)}
                required
              />
            </label>
          )}
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : metaExistente ? 'Salvar alterações' : 'Criar meta'}
          </button>
        </div>
      </form>
    </Card>
  );
}
