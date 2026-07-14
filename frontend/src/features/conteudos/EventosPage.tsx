import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Evento, StatusEvento, TipoEvento } from '../../shared/lib/types';
import styles from './EventosPage.module.css';

const COLUMNS = '1.8fr 1fr 1.4fr 1fr 1.8fr';

const TIPO_LABEL: Record<TipoEvento, string> = { AO_VIVO: 'Ao vivo', PRESENCIAL: 'Presencial' };

const STATUS_LABEL: Record<StatusEvento, { label: string; bg: string; color: string }> = {
  PROGRAMADO: { label: 'Programado', bg: 'var(--line)', color: 'var(--text-soft)' },
  AO_VIVO: { label: 'Ao vivo', bg: 'var(--info-bg)', color: 'var(--info)' },
  REALIZADO: { label: 'Realizado', bg: 'var(--success-bg)', color: 'var(--success)' },
  CANCELADO: { label: 'Cancelado', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

export function EventosPage() {
  const [status, setStatus] = useState<StatusEvento | ''>('');
  const [eventos, setEventos] = useState<Evento[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [cancelando, setCancelando] = useState<Evento | null>(null);
  const [cancelSubmitting, setCancelSubmitting] = useState(false);

  const carregar = () => {
    setEventos(null);
    apiClient
      .get<Evento[]>('/admin/eventos', { params: { status: status || undefined } })
      .then((res) => setEventos(res.data))
      .catch(() => setError('Não foi possível carregar os eventos.'));
  };

  useEffect(carregar, [status]);

  async function transicionar(id: string, novoStatus: 'AO_VIVO' | 'REALIZADO' | 'CANCELADO') {
    try {
      await apiClient.patch(`/admin/eventos/${id}/status`, { novoStatus });
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível concluir a ação.'));
    }
  }

  async function confirmarCancelamento() {
    if (!cancelando) return;
    setCancelSubmitting(true);
    await transicionar(cancelando.id, 'CANCELADO');
    setCancelSubmitting(false);
    setCancelando(null);
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusEvento | '')}>
          <option value="">Todos os status</option>
          {(Object.keys(STATUS_LABEL) as StatusEvento[]).map((s) => (
            <option key={s} value={s}>{STATUS_LABEL[s].label}</option>
          ))}
        </select>
        <button className={styles.newButton} onClick={() => setCriando(true)}>
          <span style={{ fontSize: 16 }}>+</span>Novo evento
        </button>
      </div>

      {criando && <EventoForm onSalvo={() => { setCriando(false); carregar(); }} onCancelar={() => setCriando(false)} />}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Título', 'Tipo', 'Data/Hora', 'Status', 'Ações']}>
        {eventos === null && !error && <div className={styles.loading}>Carregando…</div>}
        {eventos?.length === 0 && <div className={styles.loading}>Nenhum evento encontrado.</div>}
        {eventos?.map((ev) => {
          const st = STATUS_LABEL[ev.status];
          return (
            <DataGridRow key={ev.id} columns={COLUMNS}>
              <div className={styles.strong}>{ev.titulo}</div>
              <div className={styles.muted}>{TIPO_LABEL[ev.tipo]}</div>
              <div className={styles.muted}>{formatarDataHora(ev.dataHora)}</div>
              <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
              <div className={styles.acoes}>
                {ev.status === 'PROGRAMADO' && (
                  <button className={styles.actionButton} onClick={() => transicionar(ev.id, 'AO_VIVO')}>Iniciar</button>
                )}
                {ev.status === 'AO_VIVO' && (
                  <button className={styles.actionButton} onClick={() => transicionar(ev.id, 'REALIZADO')}>Finalizar</button>
                )}
                {(ev.status === 'PROGRAMADO' || ev.status === 'AO_VIVO') && (
                  <button className={styles.actionButtonDanger} onClick={() => setCancelando(ev)}>Cancelar</button>
                )}
                {(ev.status === 'REALIZADO' || ev.status === 'CANCELADO') && <span className={styles.muted}>—</span>}
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>

      {cancelando && (
        <ConfirmDialog
          title="Cancelar evento?"
          message={`"${cancelando.titulo}" (${formatarDataHora(cancelando.dataHora)}) será marcado como cancelado. Mentorados já inscritos verão o evento como cancelado. Essa ação não pode ser desfeita.`}
          confirmLabel="Cancelar evento"
          cancelLabel="Voltar"
          submitting={cancelSubmitting}
          onConfirm={confirmarCancelamento}
          onCancel={() => setCancelando(null)}
        />
      )}
    </div>
  );
}

function EventoForm({ onSalvo, onCancelar }: { onSalvo: () => void; onCancelar: () => void }) {
  const [titulo, setTitulo] = useState('');
  const [tipo, setTipo] = useState<TipoEvento>('AO_VIVO');
  const [tema, setTema] = useState('');
  const [dataHora, setDataHora] = useState('');
  const [local, setLocal] = useState('');
  const [linkOnline, setLinkOnline] = useState('');
  const [vagas, setVagas] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/eventos', {
        titulo, tipo, tema: tema || null,
        dataHora: dataHora ? new Date(dataHora).toISOString() : null,
        local: local || null, linkOnline: linkOnline || null,
        vagas: vagas ? Number(vagas) : null,
      });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o evento.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Novo evento</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Título
            <input className={styles.textInput} value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Tipo
            <select className={styles.select} value={tipo} onChange={(e) => setTipo(e.target.value as TipoEvento)}>
              <option value="AO_VIVO">Ao vivo</option>
              <option value="PRESENCIAL">Presencial</option>
            </select>
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Tema (opcional)
            <input className={styles.textInput} value={tema} onChange={(e) => setTema(e.target.value)} />
          </label>
          <label className={styles.formField}>
            Data e hora
            <input className={styles.textInput} type="datetime-local" value={dataHora} onChange={(e) => setDataHora(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Vagas (opcional)
            <input className={styles.textInput} type="number" min="1" value={vagas} onChange={(e) => setVagas(e.target.value)} />
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Link online (opcional)
            <input className={styles.textInput} value={linkOnline} onChange={(e) => setLinkOnline(e.target.value)} placeholder="https://meet.google.com/..." />
          </label>
          <label className={styles.formField}>
            Local presencial (opcional)
            <input className={styles.textInput} value={local} onChange={(e) => setLocal(e.target.value)} />
          </label>
        </div>
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </Card>
  );
}
