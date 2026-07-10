import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Lead, MentoradoAdmin, MentoradoCriado, Plano, StatusMentorado } from '../../shared/lib/types';
import styles from './MentoradosListaPage.module.css';

const COLUMNS = '1.4fr 1.6fr 1.2fr 1fr 1fr 1.6fr';

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito',
  BASICO: 'Básico',
  ESSENCIAL: 'Essencial',
  PROFISSIONAL: 'Profissional',
};

const STATUS_LABEL: Record<StatusMentorado, { label: string; bg: string; color: string }> = {
  ATIVO: { label: 'Ativo', bg: 'var(--success-bg)', color: 'var(--success)' },
  INATIVO: { label: 'Inativo', bg: 'var(--line)', color: 'var(--text-soft)' },
};

export function MentoradosListaPage() {
  const [plano, setPlano] = useState<Plano | ''>('');
  const [status, setStatus] = useState<StatusMentorado | ''>('');
  const [busca, setBusca] = useState('');
  const [mentorados, setMentorados] = useState<MentoradoAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState<MentoradoAdmin | null>(null);
  const [criando, setCriando] = useState(false);
  const [criado, setCriado] = useState<MentoradoCriado | null>(null);

  const carregar = () => {
    setMentorados(null);
    apiClient
      .get<MentoradoAdmin[]>('/admin/mentorados', { params: { plano: plano || undefined, status: status || undefined, busca: busca || undefined } })
      .then((res) => setMentorados(res.data))
      .catch(() => setError('Não foi possível carregar os mentorados.'));
  };

  useEffect(carregar, [plano, status, busca]);

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <select className={styles.select} value={plano} onChange={(e) => setPlano(e.target.value as Plano | '')}>
            <option value="">Todos os planos</option>
            {(Object.keys(PLANO_LABEL) as Plano[]).map((p) => (
              <option key={p} value={p}>{PLANO_LABEL[p]}</option>
            ))}
          </select>
          <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusMentorado | '')}>
            <option value="">Todos os status</option>
            <option value="ATIVO">Ativo</option>
            <option value="INATIVO">Inativo</option>
          </select>
          <input className={styles.textInput} placeholder="Buscar por nome…" value={busca} onChange={(e) => setBusca(e.target.value)} />
        </div>
        <button className={styles.newButton} onClick={() => setCriando(true)}>
          <span style={{ fontSize: 16 }}>+</span>Criar a partir de um lead
        </button>
      </div>

      {criando && (
        <CriarMentoradoForm
          onCriado={(res) => { setCriando(false); setCriado(res); carregar(); }}
          onCancelar={() => setCriando(false)}
        />
      )}

      {criado && (
        <Card style={{ padding: 20, marginBottom: 16, borderColor: 'var(--gold)' }}>
          <div className={styles.formTitle}>Mentorado criado: {criado.nome}</div>
          <p className={styles.muted}>
            Ainda não há envio automático de e-mail — repasse esta senha temporária manualmente pro
            mentorado. Ela não pode ser recuperada depois de fechar esta tela.
          </p>
          <div className={styles.credenciais}>
            <div><strong>E-mail:</strong> {criado.email}</div>
            <div><strong>Senha temporária:</strong> <code>{criado.senhaTemporaria}</code></div>
          </div>
          <div className={styles.formActions}>
            <button className={styles.actionButton} onClick={() => setCriado(null)}>Entendi</button>
          </div>
        </Card>
      )}

      {editando && (
        <EditarMentoradoForm
          mentorado={editando}
          onSalvo={() => { setEditando(null); carregar(); }}
          onCancelar={() => setEditando(null)}
        />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Nome', 'Negócio', 'E-mail', 'Plano', 'Status', 'Ações']}>
        {mentorados === null && !error && <div className={styles.loading}>Carregando…</div>}
        {mentorados?.length === 0 && <div className={styles.loading}>Nenhum mentorado encontrado.</div>}
        {mentorados?.map((m) => {
          const st = STATUS_LABEL[m.status];
          return (
            <DataGridRow key={m.id} columns={COLUMNS} testId={`mentorado-row-${m.id}`}>
              <div className={styles.strong}>{m.nome}</div>
              <div className={styles.muted}>{m.negocio ?? '—'}</div>
              <div className={`${styles.muted} ${styles.email}`}>{m.email}</div>
              <div className={styles.muted}>{PLANO_LABEL[m.plano]}</div>
              <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
              <div className={styles.acoes}>
                <button className={styles.actionButton} onClick={() => setEditando(m)}>Editar</button>
                {m.status === 'ATIVO' ? (
                  <ToggleStatusButton mentoradoId={m.id} acao="desativar" label="Desativar" onFeito={carregar} />
                ) : (
                  <ToggleStatusButton mentoradoId={m.id} acao="ativar" label="Ativar" onFeito={carregar} />
                )}
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>
    </div>
  );
}

function ToggleStatusButton({ mentoradoId, acao, label, onFeito }: {
  mentoradoId: string; acao: 'ativar' | 'desativar'; label: string; onFeito: () => void;
}) {
  const [submitting, setSubmitting] = useState(false);
  async function handleClick() {
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/mentorados/${mentoradoId}/${acao}`);
      onFeito();
    } finally {
      setSubmitting(false);
    }
  }
  return (
    <button className={styles.actionButtonDanger} onClick={handleClick} disabled={submitting}>
      {label}
    </button>
  );
}

function EditarMentoradoForm({ mentorado, onSalvo, onCancelar }: {
  mentorado: MentoradoAdmin; onSalvo: () => void; onCancelar: () => void;
}) {
  const [nome, setNome] = useState(mentorado.nome);
  const [negocio, setNegocio] = useState(mentorado.negocio ?? '');
  const [plano, setPlano] = useState<Plano>(mentorado.plano);
  const [vencimentoPlano, setVencimentoPlano] = useState(mentorado.vencimentoPlano ?? '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.put(`/admin/mentorados/${mentorado.id}`, {
        nome, negocio: negocio || null, plano, vencimentoPlano: vencimentoPlano || null,
      });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Editar mentorado</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Nome
            <input className={styles.textInput} value={nome} onChange={(e) => setNome(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Negócio
            <input className={styles.textInput} value={negocio} onChange={(e) => setNegocio(e.target.value)} />
          </label>
          <label className={styles.formField}>
            Plano
            <select className={styles.select} value={plano} onChange={(e) => setPlano(e.target.value as Plano)}>
              {(Object.keys(PLANO_LABEL) as Plano[]).map((p) => (
                <option key={p} value={p}>{PLANO_LABEL[p]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Vencimento do plano
            <input className={styles.textInput} type="date" value={vencimentoPlano} onChange={(e) => setVencimentoPlano(e.target.value)} />
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

function CriarMentoradoForm({ onCriado, onCancelar }: {
  onCriado: (res: MentoradoCriado) => void; onCancelar: () => void;
}) {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [leadId, setLeadId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiClient.get<Lead[]>('/admin/comercial/leads', { params: { status: 'FECHADO' } }).then((res) => setLeads(res.data));
  }, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const res = await apiClient.post<MentoradoCriado>(`/admin/mentorados/a-partir-do-lead/${leadId}`);
      onCriado(res.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar o mentorado. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Criar mentorado a partir de um lead fechado</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.formField}>
          Lead
          <select className={styles.select} value={leadId} onChange={(e) => setLeadId(e.target.value)} required>
            <option value="">Selecione um lead fechado</option>
            {leads.map((l) => (
              <option key={l.id} value={l.id}>{l.nome} — {l.email}</option>
            ))}
          </select>
        </label>
        {leads.length === 0 && <div className={styles.muted}>Nenhum lead fechado disponível no momento.</div>}
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting || !leadId}>
            {submitting ? 'Criando…' : 'Criar mentorado'}
          </button>
        </div>
      </form>
    </Card>
  );
}
