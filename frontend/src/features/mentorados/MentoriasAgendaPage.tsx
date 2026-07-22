import { type FormEvent, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { AvisoAreaMentoradoPausada } from '../../shared/components/AvisoAreaMentoradoPausada';
import { Card } from '../../shared/components/Card';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Ata, MentoradoAdmin, Mentoria, MentorResumo, StatusMentoria, TipoMentoria } from '../../shared/lib/types';
import styles from './MentoriasAgendaPage.module.css';

const COLUMNS = '1.3fr 1fr 1.2fr 1.8fr 1fr 1.8fr';

const STATUS_LABEL: Record<StatusMentoria, { label: string; bg: string; color: string }> = {
  AGENDADA: { label: 'Agendada', bg: 'var(--line)', color: 'var(--text-soft)' },
  CONFIRMADA: { label: 'Confirmada', bg: 'var(--info-bg)', color: 'var(--info)' },
  REALIZADA: { label: 'Realizada', bg: 'var(--success-bg)', color: 'var(--success)' },
  CANCELADA: { label: 'Cancelada', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

// M28 (change request, 21/07/2026, "reorganizar lista de mentorias") — esta lista central passa a
// mostrar só GRUPO por padrão: individual/consultoria têm seu próprio mentorado, então o
// acompanhamento delas vive na página dele (MentoradoDetalhePage, seção "Mentorias"), não misturado
// aqui com todo mundo. O filtro continua trocável (Individual/Todos) pra quem realmente precisar
// ver tudo nesta tela — só o padrão mudou.
const TIPO_FILTRO_LABEL: Record<TipoMentoria, string> = {
  GRUPO: 'Grupo',
  INDIVIDUAL: 'Individual',
};

function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

export function MentoriasAgendaPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<StatusMentoria | ''>('');
  // M28 — GRUPO é o padrão (ver TIPO_FILTRO_LABEL acima); '' = Todos os tipos.
  const [tipo, setTipo] = useState<TipoMentoria | ''>('GRUPO');
  const [mentorias, setMentorias] = useState<Mentoria[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [processando, setProcessando] = useState<string | null>(null);
  const [cancelando, setCancelando] = useState<Mentoria | null>(null);
  // M28 — status/tipo mudaram de forma independente do fluxo de criação: "Nova mentoria" dispara
  // um POST assíncrono e só chama carregar() (via onCriada) quando ele termina — se o filtro de
  // tipo mudar ENQUANTO esse POST ainda está em voo, o carregar() do onCriada acaba sendo EMITIDO
  // depois do carregar() do filtro novo (por mais lento, não por ser "mais recente"), e sua
  // resposta (com o filtro ANTIGO) chegava por último e sobrescrevia a tela — achado ao vivo no
  // E2E (dashboard-admin.spec.ts: dropdown mostrava "Individual", tabela continuava com Grupo).
  // Ordem de EMISSÃO não é um proxy confiável pra "resposta mais recente" aqui; a defesa certa é
  // comparar contra o filtro ATUAL no momento em que a resposta chega, não contra um contador.
  const filtroAtualRef = useRef({ status, tipo });
  useEffect(() => {
    filtroAtualRef.current = { status, tipo };
  });

  const carregar = () => {
    const filtroDestaRequisicao = { status, tipo };
    setMentorias(null);
    apiClient
      .get<Mentoria[]>('/admin/mentorias', { params: { status: status || undefined, tipo: tipo || undefined } })
      .then((res) => {
        const atual = filtroAtualRef.current;
        if (atual.status === filtroDestaRequisicao.status && atual.tipo === filtroDestaRequisicao.tipo) {
          setMentorias(res.data);
        }
      })
      .catch(() => {
        const atual = filtroAtualRef.current;
        if (atual.status === filtroDestaRequisicao.status && atual.tipo === filtroDestaRequisicao.tipo) {
          setError('Não foi possível carregar as mentorias.');
        }
      });
  };

  useEffect(carregar, [status, tipo]);

  async function transicionar(id: string, novoStatus: 'CONFIRMADA' | 'CANCELADA') {
    setProcessando(id);
    try {
      await apiClient.patch(`/admin/mentorias/${id}/status`, { novoStatus });
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível concluir a ação.'));
    } finally {
      setProcessando(null);
    }
  }

  async function confirmarCancelamento() {
    if (!cancelando) return;
    await transicionar(cancelando.id, 'CANCELADA');
    setCancelando(null);
  }

  async function realizar(id: string) {
    setProcessando(id);
    try {
      await apiClient.post<Ata>(`/admin/mentorias/${id}/realizar`);
      navigate(`/admin/mentorados/mentorias/${id}/ata`);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível realizar a mentoria.'));
      setProcessando(null);
    }
  }

  return (
    <div>
      <AvisoAreaMentoradoPausada />
      <div className={styles.toolbar}>
        <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusMentoria | '')}>
          <option value="">Todos os status</option>
          {(Object.keys(STATUS_LABEL) as StatusMentoria[]).map((s) => (
            <option key={s} value={s}>{STATUS_LABEL[s].label}</option>
          ))}
        </select>
        {/* M28 — individual/consultoria não somem do sistema, só desta lista central por padrão;
            "Todos os tipos" continua disponível pra quem precisar ver junto. */}
        {/* Rótulo evita a palavra "Mentor" de propósito — getByLabel('Mentor') do form de Nova
            Mentoria (substring match) bateria neste select também se o texto contivesse esse
            prefixo (achado ao vivo: "mentoria" começa com "mentor"). */}
        <select
          className={styles.select}
          aria-label="Filtro de tipo (agenda)"
          value={tipo}
          onChange={(e) => setTipo(e.target.value as TipoMentoria | '')}
        >
          {(Object.keys(TIPO_FILTRO_LABEL) as TipoMentoria[]).map((t) => (
            <option key={t} value={t}>{TIPO_FILTRO_LABEL[t]}</option>
          ))}
          <option value="">Todos os tipos</option>
        </select>
        <button className={styles.newButton} onClick={() => setCriando(true)}>
          <span style={{ fontSize: 16 }}>+</span>Nova mentoria
        </button>
      </div>

      {criando && (
        <NovaMentoriaForm onCriada={() => { setCriando(false); carregar(); }} onCancelar={() => setCriando(false)} />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Data/Hora', 'Tipo', 'Mentor', 'Mentorados', 'Status', 'Ações']}>
        {mentorias === null && !error && <div className={styles.loading}>Carregando…</div>}
        {mentorias?.length === 0 && <div className={styles.loading}>Nenhuma mentoria encontrada.</div>}
        {mentorias?.map((m) => {
          const st = STATUS_LABEL[m.status];
          const emProcessamento = processando === m.id;
          return (
            <DataGridRow key={m.id} columns={COLUMNS} testId={`mentoria-row-${m.id}`}>
              <div className={styles.strong}>{formatarDataHora(m.dataHora)}</div>
              <div className={styles.muted}>{m.tipo === 'INDIVIDUAL' ? 'Individual' : 'Grupo'}</div>
              <div className={styles.muted}>{m.mentor.nome}</div>
              <div className={styles.muted}>{m.mentorados.map((mt) => mt.nome).join(', ')}</div>
              <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
              <div className={styles.acoes}>
                {m.status === 'AGENDADA' && (
                  <button className={styles.actionButton} disabled={emProcessamento} onClick={() => transicionar(m.id, 'CONFIRMADA')}>
                    Confirmar
                  </button>
                )}
                {m.status === 'CONFIRMADA' && (
                  <button className={styles.actionButton} disabled={emProcessamento} onClick={() => realizar(m.id)}>
                    Realizar
                  </button>
                )}
                {(m.status === 'AGENDADA' || m.status === 'CONFIRMADA') && (
                  <button className={styles.actionButtonDanger} disabled={emProcessamento} onClick={() => setCancelando(m)}>
                    Cancelar
                  </button>
                )}
                {m.status === 'REALIZADA' && (
                  <button className={styles.actionButton} onClick={() => navigate(`/admin/mentorados/mentorias/${m.id}/ata`)}>
                    Ver ata
                  </button>
                )}
                {m.status === 'CANCELADA' && <span className={styles.muted}>—</span>}
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>

      {cancelando && (
        <ConfirmDialog
          title="Cancelar mentoria?"
          message={`A mentoria de ${formatarDataHora(cancelando.dataHora)} com ${cancelando.mentorados.map((mt) => mt.nome).join(', ')} será marcada como cancelada. Essa ação não pode ser desfeita.`}
          confirmLabel="Cancelar mentoria"
          cancelLabel="Voltar"
          submitting={processando === cancelando.id}
          onConfirm={confirmarCancelamento}
          onCancel={() => setCancelando(null)}
        />
      )}
    </div>
  );
}

function NovaMentoriaForm({ onCriada, onCancelar }: { onCriada: () => void; onCancelar: () => void }) {
  const [tipo, setTipo] = useState<TipoMentoria>('INDIVIDUAL');
  const [mentorId, setMentorId] = useState('');
  const [mentoradoIds, setMentoradoIds] = useState<string[]>([]);
  const [data, setData] = useState('');
  const [duracaoMin, setDuracaoMin] = useState('60');
  const [linkOnline, setLinkOnline] = useState('');
  const [local, setLocal] = useState('');
  const [mentores, setMentores] = useState<MentorResumo[]>([]);
  const [mentorados, setMentorados] = useState<MentoradoAdmin[]>([]);
  const [buscaMentorado, setBuscaMentorado] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiClient.get<MentorResumo[]>('/admin/mentorias/mentores')
      .then((res) => setMentores(res.data))
      .catch(() => setError('Não foi possível carregar a lista de mentores.'));
    apiClient.get<MentoradoAdmin[]>('/admin/mentorados', { params: { status: 'ATIVO' } })
      .then((res) => setMentorados(res.data))
      .catch(() => setError('Não foi possível carregar a lista de mentorados.'));
  }, []);

  const mentoradosFiltrados = mentorados.filter((m) => m.nome.toLowerCase().includes(buscaMentorado.trim().toLowerCase()));

  function toggleMentorado(id: string) {
    if (tipo === 'INDIVIDUAL') {
      setMentoradoIds([id]);
      return;
    }
    setMentoradoIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/mentorias', {
        tipo,
        mentoradoIds,
        mentorId,
        dataHora: data ? new Date(data).toISOString() : null,
        duracaoMin: Number(duracaoMin),
        linkOnline: linkOnline || null,
        local: local || null,
      });
      onCriada();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar a mentoria. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Nova mentoria</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Tipo
            <select className={styles.select} value={tipo} onChange={(e) => { setTipo(e.target.value as TipoMentoria); setMentoradoIds([]); }}>
              <option value="INDIVIDUAL">Individual</option>
              <option value="GRUPO">Grupo</option>
            </select>
          </label>
          <label className={styles.formField}>
            Mentor
            <select className={styles.select} value={mentorId} onChange={(e) => setMentorId(e.target.value)} required>
              <option value="">{mentores.length === 0 ? 'Nenhum mentor cadastrado' : 'Selecione'}</option>
              {mentores.map((m) => (
                <option key={m.id} value={m.id}>{m.nome}</option>
              ))}
            </select>
            {mentores.length === 0 && (
              <div className={styles.muted}>
                Nenhum colaborador de Gestão de Performance ou Admin cadastrado — crie um em Time.
              </div>
            )}
          </label>
        </div>

        <div className={styles.formField}>
          {tipo === 'INDIVIDUAL' ? 'Mentorado' : 'Mentorados (selecione um ou mais)'}
          <input
            className={styles.textInput}
            placeholder="Buscar mentorado..."
            value={buscaMentorado}
            onChange={(e) => setBuscaMentorado(e.target.value)}
          />
          <div className={styles.checkboxList}>
            {mentoradosFiltrados.length === 0 && <div className={styles.checkboxEmpty}>Nenhum mentorado encontrado.</div>}
            {mentoradosFiltrados.map((m) => (
              <label key={m.id} className={styles.checkboxItem}>
                <input
                  type={tipo === 'INDIVIDUAL' ? 'radio' : 'checkbox'}
                  name="mentorado"
                  checked={mentoradoIds.includes(m.id)}
                  onChange={() => toggleMentorado(m.id)}
                />
                {m.nome}
              </label>
            ))}
          </div>
        </div>

        <div className={styles.formRow}>
          <label className={styles.formField}>
            Data e hora
            <input className={styles.textInput} type="datetime-local" value={data} onChange={(e) => setData(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Duração (min)
            <input className={styles.textInput} type="number" min="15" step="15" value={duracaoMin} onChange={(e) => setDuracaoMin(e.target.value)} required />
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
          <button type="submit" className={styles.actionButton} disabled={submitting || mentoradoIds.length === 0}>
            {submitting ? 'Criando…' : 'Criar mentoria'}
          </button>
        </div>
      </form>
    </Card>
  );
}
