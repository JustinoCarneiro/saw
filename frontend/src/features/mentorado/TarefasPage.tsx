import { type FormEvent, type ReactElement, useEffect, useRef, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { ICON_PROPS } from '../../shared/components/iconProps';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Meta, Prioridade, ResumoTarefas, StatusTarefa, Tarefa } from '../../shared/lib/types';
import styles from './TarefasPage.module.css';

const COLUMNS = '1.8fr 1.3fr 1.2fr 1fr 1.1fr 1.6fr';

const FILTROS: { label: string; status: StatusTarefa | '' }[] = [
  { label: 'Todas', status: '' },
  { label: 'Pendentes', status: 'PENDENTE' },
  { label: 'Em andamento', status: 'EM_ANDAMENTO' },
  { label: 'Concluídas', status: 'CONCLUIDA' },
];

// Achado de UX: setas Unicode (↑→↓) como indicador de prioridade destoavam do traço linear
// (ICON_PROPS) usado no resto do app.
function seta(d: string): ReactElement {
  return (
    <svg {...ICON_PROPS} width={13} height={13}>
      <path d={d} />
    </svg>
  );
}

const PRIORIDADE_INFO: Record<Prioridade, { label: string; color: string; arrow: ReactElement }> = {
  ALTA: { label: 'Alta', color: 'var(--danger)', arrow: seta('M12 19V5M6 11l6-6 6 6') },
  MEDIA: { label: 'Média', color: 'var(--gold)', arrow: seta('M5 12h14M13 6l6 6-6 6') },
  BAIXA: { label: 'Baixa', color: 'var(--text-faint)', arrow: seta('M12 5v14M18 13l-6 6-6-6') },
};

function statusPill(t: Tarefa): { label: string; bg: string; color: string } {
  if (t.atrasada) return { label: 'Atrasada', bg: 'var(--danger-bg)', color: 'var(--danger)' };
  if (t.status === 'CONCLUIDA') return { label: 'Concluída', bg: 'var(--success-bg)', color: 'var(--success)' };
  if (t.status === 'EM_ANDAMENTO') return { label: 'Em andamento', bg: 'var(--info-bg)', color: 'var(--info)' };
  return { label: 'Pendente', bg: 'var(--line)', color: 'var(--text-soft)' };
}

function formatarPrazo(iso: string | null): string {
  // Encaminhamentos antigos e os gerados por ata não têm prazo (ver ROADMAP.md M10) — sem este
  // guard, `new Date(null + 'T00:00:00')` produz "Invalid Date" visível pro usuário.
  if (!iso) return 'Sem prazo';
  return new Date(iso + 'T00:00:00').toLocaleDateString('pt-BR');
}

export function TarefasPage() {
  const [filtro, setFiltro] = useState<StatusTarefa | ''>('');
  const [busca, setBusca] = useState('');
  const [tarefas, setTarefas] = useState<Tarefa[] | null>(null);
  const [resumo, setResumo] = useState<ResumoTarefas | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formulario, setFormulario] = useState<'fechado' | 'criar' | Tarefa>('fechado');
  const [processando, setProcessando] = useState<string | null>(null);
  // Ações de status (Iniciar/Concluir/Reabrir) chamam carregar() diretamente, fora do useEffect
  // de filtro/busca — clicar rápido o suficiente dispara dois fetches em paralelo cujas respostas
  // podem chegar fora de ordem, e a mais antiga (com dado já desatualizado) sobrescreveria a mais
  // nova. O requestId garante que só a resposta do fetch MAIS RECENTE é aplicada.
  const requestIdRef = useRef(0);

  const carregar = () => {
    const requestId = ++requestIdRef.current;
    setTarefas(null);
    apiClient
      .get<Tarefa[]>('/mentorado/tarefas', { params: { status: filtro || undefined, busca: busca || undefined } })
      .then((res) => { if (requestId === requestIdRef.current) setTarefas(res.data); })
      .catch((err) => { if (requestId === requestIdRef.current) setError(getApiErrorMessage(err, 'Não foi possível carregar seus encaminhamentos.')); });
    apiClient.get<ResumoTarefas>('/mentorado/tarefas/resumo')
      .then((res) => { if (requestId === requestIdRef.current) setResumo(res.data); });
  };

  useEffect(carregar, [filtro, busca]);

  async function avancarStatus(id: string, novoStatus: 'EM_ANDAMENTO' | 'CONCLUIDA' | 'PENDENTE') {
    setProcessando(id);
    try {
      await apiClient.patch(`/mentorado/tarefas/${id}/status`, { novoStatus });
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível concluir a ação.'));
    } finally {
      setProcessando(null);
    }
  }

  return (
    <div>
      <h1 className={styles.title}>Encaminhamentos</h1>
      <p className={styles.subtitle}>Organize suas ações diárias e avance com consistência.</p>

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
        <div className={styles.toolbarRight}>
          <input
            className={styles.searchInput}
            placeholder="Buscar encaminhamentos..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
          <button className={styles.newButton} onClick={() => setFormulario('criar')}>
            <span style={{ fontSize: 16 }}>+</span>Novo encaminhamento
          </button>
        </div>
      </div>

      {formulario !== 'fechado' && (
        <TarefaForm
          tarefaExistente={formulario === 'criar' ? null : formulario}
          onSalva={() => { setFormulario('fechado'); carregar(); }}
          onCancelar={() => setFormulario('fechado')}
        />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Encaminhamento', 'Meta relacionada', 'Prazo', 'Prioridade', 'Status', 'Ações']}>
        {tarefas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {tarefas?.length === 0 && <div className={styles.loading}>Nenhum encaminhamento encontrado.</div>}
        {tarefas?.map((t) => {
          const pill = statusPill(t);
          const prio = PRIORIDADE_INFO[t.prioridade];
          const emProcessamento = processando === t.id;
          return (
            <DataGridRow key={t.id} columns={COLUMNS} testId={`tarefa-row-${t.id}`}>
              <div className={styles.strong}>{t.titulo}</div>
              <div className={styles.muted}>{t.metaRelacionada?.titulo ?? '—'}</div>
              <div className={styles.muted}>{formatarPrazo(t.prazo)}</div>
              <div className={styles.prio} style={{ color: prio.color }}>{prio.arrow} {prio.label}</div>
              <div><Pill bg={pill.bg} color={pill.color}>{pill.label}</Pill></div>
              <div className={styles.acoes}>
                {t.status !== 'CONCLUIDA' && (
                  <button className={styles.actionButton} disabled={emProcessamento} onClick={() => setFormulario(t)}>
                    Editar
                  </button>
                )}
                {t.status === 'PENDENTE' && (
                  <button className={styles.actionButton} disabled={emProcessamento} onClick={() => avancarStatus(t.id, 'EM_ANDAMENTO')}>
                    Iniciar
                  </button>
                )}
                {t.status !== 'CONCLUIDA' && (
                  <button className={styles.actionButton} disabled={emProcessamento} onClick={() => avancarStatus(t.id, 'CONCLUIDA')}>
                    Concluir
                  </button>
                )}
                {t.status === 'CONCLUIDA' && (
                  <button className={styles.actionButtonDanger} disabled={emProcessamento} onClick={() => avancarStatus(t.id, 'PENDENTE')}>
                    Reabrir
                  </button>
                )}
              </div>
            </DataGridRow>
          );
        })}
        {resumo && (
          <DataGridRow columns={COLUMNS}>
            <div>
              <div className={styles.strong}>Resumo geral</div>
              <div className={styles.muted}>{resumo.total} encaminhamento(s) no total.</div>
            </div>
            <div />
            <div>
              <div className={styles.summaryValue}>{resumo.pendentes}</div>
              <div className={styles.muted}>Pendentes</div>
            </div>
            <div>
              <div className={styles.summaryValue} style={{ color: 'var(--info)' }}>{resumo.emAndamento}</div>
              <div className={styles.muted}>Em andamento</div>
            </div>
            <div>
              <div className={styles.summaryValue} style={{ color: 'var(--success)' }}>{resumo.concluidas}</div>
              <div className={styles.muted}>Concluídas</div>
            </div>
            <div />
          </DataGridRow>
        )}
      </DataGrid>
    </div>
  );
}

function TarefaForm({ tarefaExistente, onSalva, onCancelar }: {
  tarefaExistente: Tarefa | null;
  onSalva: () => void;
  onCancelar: () => void;
}) {
  const [titulo, setTitulo] = useState(tarefaExistente?.titulo ?? '');
  const [prazo, setPrazo] = useState(tarefaExistente?.prazo ?? '');
  const [prioridade, setPrioridade] = useState<Prioridade>(tarefaExistente?.prioridade ?? 'MEDIA');
  const [metaId, setMetaId] = useState(tarefaExistente?.metaRelacionada?.id ?? '');
  const [metas, setMetas] = useState<Meta[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiClient.get<Meta[]>('/mentorado/metas', { params: { status: 'ATIVA' } }).then((res) => setMetas(res.data));
  }, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const payload = { titulo, prazo, prioridade, metaId: metaId || null };
      if (tarefaExistente) {
        await apiClient.put(`/mentorado/tarefas/${tarefaExistente.id}`, payload);
      } else {
        await apiClient.post('/mentorado/tarefas', payload);
      }
      onSalva();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o encaminhamento. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>{tarefaExistente ? 'Editar encaminhamento' : 'Novo encaminhamento'}</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.formField}>
          Título
          <input className={styles.textInput} value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
        </label>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Prazo
            <input className={styles.textInput} type="date" value={prazo} onChange={(e) => setPrazo(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Prioridade
            <select className={styles.select} value={prioridade} onChange={(e) => setPrioridade(e.target.value as Prioridade)}>
              <option value="ALTA">Alta</option>
              <option value="MEDIA">Média</option>
              <option value="BAIXA">Baixa</option>
            </select>
          </label>
          <label className={styles.formField}>
            Meta relacionada (opcional)
            <select className={styles.select} value={metaId} onChange={(e) => setMetaId(e.target.value)}>
              <option value="">Nenhuma</option>
              {metas.map((m) => (
                <option key={m.id} value={m.id}>{m.titulo}</option>
              ))}
            </select>
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : tarefaExistente ? 'Salvar alterações' : 'Criar encaminhamento'}
          </button>
        </div>
      </form>
    </Card>
  );
}
