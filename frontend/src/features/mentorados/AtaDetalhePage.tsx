import { type FormEvent, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Ata, Conteudo, Mentoria, StatusProcessamentoAta, SugestaoEncaminhamento } from '../../shared/lib/types';
import styles from './AtaDetalhePage.module.css';

const STATUS_PROC_LABEL: Record<StatusProcessamentoAta, { label: string; bg: string; color: string }> = {
  SEM_AUDIO: { label: 'Sem áudio', bg: 'var(--line)', color: 'var(--text-soft)' },
  PROCESSANDO: { label: 'Processando…', bg: 'var(--warning-bg)', color: 'var(--warning)' },
  CONCLUIDO: { label: 'IA concluída', bg: 'var(--success-bg)', color: 'var(--success)' },
  FALHA: { label: 'Falha no processamento', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

export function AtaDetalhePage() {
  const { mentoriaId } = useParams<{ mentoriaId: string }>();
  const [ata, setAta] = useState<Ata | null>(null);
  const [mentoria, setMentoria] = useState<Mentoria | null>(null);
  const [error, setError] = useState<string | null>(null);
  const intervalRef = useRef<number | undefined>(undefined);

  const carregar = () => {
    if (!mentoriaId) return;
    apiClient.get<Ata>(`/admin/mentorias/${mentoriaId}/ata`)
      .then((res) => setAta(res.data))
      .catch(() => setError('Não foi possível carregar a ata.'));
    apiClient.get<Mentoria>(`/admin/mentorias/${mentoriaId}`)
      .then((res) => setMentoria(res.data))
      .catch(() => setError('Não foi possível carregar a mentoria.'));
  };

  useEffect(carregar, [mentoriaId]);

  useEffect(() => {
    if (ata?.statusProcessamento === 'PROCESSANDO') {
      // Sem WebSocket no stack (ver ROADMAP.md M06) — polling simples é suficiente pro volume
      // baixo desse fluxo (uma mentoria de cada vez).
      intervalRef.current = window.setInterval(carregar, 3000);
      return () => window.clearInterval(intervalRef.current);
    }
    return undefined;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ata?.statusProcessamento]);

  if (!mentoriaId) return null;
  if (error) return <div className={styles.error}>{error}</div>;
  if (!ata || !mentoria) return <div className={styles.loading}>Carregando…</div>;

  const stProc = STATUS_PROC_LABEL[ata.statusProcessamento];
  const publicada = ata.status === 'PUBLICADA';

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <div className={styles.pills}>
          <Pill bg={stProc.bg} color={stProc.color}>{stProc.label}</Pill>
          <Pill bg={publicada ? 'var(--success-bg)' : 'var(--line)'} color={publicada ? 'var(--success)' : 'var(--text-soft)'}>
            {publicada ? 'Publicada' : 'Rascunho'}
          </Pill>
        </div>
      </div>

      {ata.erroProcessamento && (
        <Card style={{ padding: 16, marginBottom: 16, borderColor: 'var(--danger)' }}>
          <div className={styles.strong}>Falha no processamento de IA</div>
          <div className={styles.muted}>{ata.erroProcessamento}</div>
          <div className={styles.muted} style={{ marginTop: 6 }}>
            Você pode tentar subir o áudio de novo, ou escrever o resumo manualmente abaixo.
          </div>
        </Card>
      )}

      {!publicada && (
        <UploadAudioForm mentoriaId={mentoriaId} desabilitado={ata.statusProcessamento === 'PROCESSANDO'} onEnviado={carregar} />
      )}

      {mentoria.tipo === 'GRUPO' && <PresencaCard mentoriaId={mentoriaId} mentoria={mentoria} onSalvo={carregar} />}

      <ResumoCard mentoriaId={mentoriaId} ata={ata} onSalvo={carregar} />

      <DecisoesCard mentoriaId={mentoriaId} ata={ata} onSalvo={carregar} />

      <MateriaisCard mentoriaId={mentoriaId} mentoria={mentoria} podeEditar={!publicada} onSalvo={carregar} />

      {ata.sugestoes.length > 0 && (
        <SugestoesCard mentoriaId={mentoriaId} sugestoes={ata.sugestoes} podeEditar={!publicada} onSalvo={carregar} />
      )}

      {ata.transcricao && (
        <Card style={{ padding: 20, marginBottom: 16 }}>
          <div className={styles.sectionTitle}>Transcrição</div>
          <div className={styles.transcricao}>{ata.transcricao}</div>
        </Card>
      )}

      {!publicada && (
        <PublicarButton mentoriaId={mentoriaId} desabilitado={ata.statusProcessamento === 'PROCESSANDO'} onPublicado={carregar} />
      )}
    </div>
  );
}

// M28 (change request, 21/07/2026) — "colar transcrição do Google Meet": aditivo, não substitui o
// upload de áudio (Whisper). O mentor escolhe qual caminho usar; os dois terminam no mesmo
// pipeline de IA (resumo/decisões/sugestões via Claude) e no mesmo polling de status abaixo.
type ModoTranscricao = 'AUDIO' | 'TEXTO';

function UploadAudioForm({ mentoriaId, desabilitado, onEnviado }: {
  mentoriaId: string; desabilitado: boolean; onEnviado: () => void;
}) {
  const [modo, setModo] = useState<ModoTranscricao>('AUDIO');
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [transcricaoColada, setTranscricaoColada] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function handleSubmitAudio(e: FormEvent) {
    e.preventDefault();
    if (!arquivo) return;
    setError(null);
    setEnviando(true);
    try {
      const formData = new FormData();
      formData.append('arquivo', arquivo);
      await apiClient.post(`/admin/mentorias/${mentoriaId}/ata/audio`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setArquivo(null);
      onEnviado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível enviar o áudio.'));
    } finally {
      setEnviando(false);
    }
  }

  async function handleSubmitTexto(e: FormEvent) {
    e.preventDefault();
    if (!transcricaoColada.trim()) return;
    setError(null);
    setEnviando(true);
    try {
      await apiClient.post(`/admin/mentorias/${mentoriaId}/ata/transcricao`, { transcricao: transcricaoColada });
      setTranscricaoColada('');
      onEnviado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível processar a transcrição.'));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Transcrição da mentoria</div>
      <p className={styles.muted}>
        Suba a gravação ou cole a transcrição do Google Meet — os dois caminhos geram
        automaticamente a transcrição/resumo/decisões/sugestões (revisão humana obrigatória antes
        de publicar).
      </p>
      <div className={styles.modoTabs} role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={modo === 'AUDIO'}
          className={`${styles.modoTab} ${modo === 'AUDIO' ? styles.modoTabActive : ''}`}
          onClick={() => setModo('AUDIO')}
          disabled={desabilitado || enviando}
        >
          Subir áudio
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={modo === 'TEXTO'}
          className={`${styles.modoTab} ${modo === 'TEXTO' ? styles.modoTabActive : ''}`}
          onClick={() => setModo('TEXTO')}
          disabled={desabilitado || enviando}
        >
          Colar transcrição
        </button>
      </div>
      {modo === 'AUDIO' ? (
        <form onSubmit={handleSubmitAudio} className={styles.uploadForm}>
          <input
            type="file"
            accept="audio/*"
            onChange={(e) => setArquivo(e.target.files?.[0] ?? null)}
            disabled={desabilitado || enviando}
          />
          <button type="submit" className={styles.actionButton} disabled={!arquivo || desabilitado || enviando}>
            {enviando ? 'Enviando…' : 'Enviar áudio'}
          </button>
        </form>
      ) : (
        <form onSubmit={handleSubmitTexto} className={styles.textForm}>
          <textarea
            className={styles.textarea}
            rows={6}
            placeholder="Cole aqui a transcrição gerada pelo Google Meet…"
            value={transcricaoColada}
            onChange={(e) => setTranscricaoColada(e.target.value)}
            disabled={desabilitado || enviando}
          />
          <div className={styles.formActions}>
            <button
              type="submit"
              className={styles.actionButton}
              disabled={!transcricaoColada.trim() || desabilitado || enviando}
            >
              {enviando ? 'Processando…' : 'Processar transcrição'}
            </button>
          </div>
        </form>
      )}
      {error && <div className={styles.error}>{error}</div>}
    </Card>
  );
}

// E17/M27 (change request pós-MVP, 19/07/2026) — presença só faz sentido em mentoria GRUPO (ver
// ROADMAP.md § "Blueprint (M27)"); marcada depois da sessão, não em tempo real, então não trava
// nenhum outro passo da ata. Só alimenta a frequência exibida no Painel Consolidado — não afeta
// ranking/progresso.
function PresencaCard({ mentoriaId, mentoria, onSalvo }: { mentoriaId: string; mentoria: Mentoria; onSalvo: () => void }) {
  const [presencas, setPresencas] = useState<Record<string, boolean>>(
    () => Object.fromEntries(mentoria.mentorados.map((mt) => [mt.id, mt.presente ?? false])),
  );
  const [error, setError] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [salvo, setSalvo] = useState(false);

  useEffect(() => {
    setPresencas(Object.fromEntries(mentoria.mentorados.map((mt) => [mt.id, mt.presente ?? false])));
  }, [mentoria.mentorados]);

  function toggle(id: string) {
    setSalvo(false);
    setPresencas((prev) => ({ ...prev, [id]: !prev[id] }));
  }

  async function salvar() {
    setError(null);
    setSalvo(false);
    setSalvando(true);
    try {
      await apiClient.patch(`/admin/mentorias/${mentoriaId}/presencas`, {
        presencas: mentoria.mentorados.map((mt) => ({ mentoradoId: mt.id, presente: presencas[mt.id] ?? false })),
      });
      setSalvo(true);
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar a presença.'));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }} testId="presenca-card">
      <div className={styles.sectionTitle}>Presença</div>
      <p className={styles.muted}>Marque quem participou desta mentoria em grupo.</p>
      <div className={styles.checkboxList}>
        {mentoria.mentorados.map((mt) => (
          <label key={mt.id} className={styles.checkboxItem}>
            <input type="checkbox" checked={presencas[mt.id] ?? false} onChange={() => toggle(mt.id)} />
            {mt.nome}
          </label>
        ))}
      </div>
      {error && <div className={styles.error}>{error}</div>}
      <div className={styles.formActions}>
        {salvo && !salvando && <span className={styles.muted}>Salvo.</span>}
        <button className={styles.actionButton} onClick={salvar} disabled={salvando}>
          {salvando ? 'Salvando…' : 'Salvar presença'}
        </button>
      </div>
    </Card>
  );
}

function ResumoCard({ mentoriaId, ata, onSalvo }: { mentoriaId: string; ata: Ata; onSalvo: () => void }) {
  const [resumo, setResumo] = useState(ata.resumo ?? '');
  const [error, setError] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const publicada = ata.status === 'PUBLICADA';

  useEffect(() => setResumo(ata.resumo ?? ''), [ata.resumo]);

  async function salvar() {
    setError(null);
    setSalvando(true);
    try {
      await apiClient.patch(`/admin/mentorias/${mentoriaId}/ata`, { resumo });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o resumo.'));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Resumo</div>
      <textarea
        className={styles.textarea}
        rows={5}
        value={resumo}
        onChange={(e) => setResumo(e.target.value)}
        disabled={publicada}
        placeholder="Escreva o resumo da mentoria (ou aguarde o rascunho da IA)…"
      />
      {error && <div className={styles.error}>{error}</div>}
      {!publicada && (
        <div className={styles.formActions}>
          <button className={styles.actionButton} onClick={salvar} disabled={salvando || resumo === (ata.resumo ?? '')}>
            {salvando ? 'Salvando…' : 'Salvar resumo'}
          </button>
        </div>
      )}
    </Card>
  );
}

// Change request 17/07/2026 ("campo Decisões na ata") — mesmo padrão de ResumoCard, campo
// distinto e independente (edição de um não mexe no outro).
function DecisoesCard({ mentoriaId, ata, onSalvo }: { mentoriaId: string; ata: Ata; onSalvo: () => void }) {
  const [decisoes, setDecisoes] = useState(ata.decisoes ?? '');
  const [error, setError] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const publicada = ata.status === 'PUBLICADA';

  useEffect(() => setDecisoes(ata.decisoes ?? ''), [ata.decisoes]);

  async function salvar() {
    setError(null);
    setSalvando(true);
    try {
      await apiClient.patch(`/admin/mentorias/${mentoriaId}/ata/decisoes`, { decisoes });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar as decisões.'));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Decisões</div>
      <textarea
        className={styles.textarea}
        rows={4}
        value={decisoes}
        onChange={(e) => setDecisoes(e.target.value)}
        disabled={publicada}
        placeholder="Registre as decisões tomadas na mentoria (ou aguarde o rascunho da IA)…"
      />
      {error && <div className={styles.error}>{error}</div>}
      {!publicada && (
        <div className={styles.formActions}>
          <button className={styles.actionButton} onClick={salvar} disabled={salvando || decisoes === (ata.decisoes ?? '')}>
            {salvando ? 'Salvando…' : 'Salvar decisões'}
          </button>
        </div>
      )}
    </Card>
  );
}

function MateriaisCard({ mentoriaId, mentoria, podeEditar, onSalvo }: {
  mentoriaId: string; mentoria: Mentoria; podeEditar: boolean; onSalvo: () => void;
}) {
  const [conteudos, setConteudos] = useState<Conteudo[] | null>(null);
  const [selecionados, setSelecionados] = useState<Set<string>>(
    () => new Set(mentoria.materiaisRecomendados.map((m) => m.id)),
  );
  const [busca, setBusca] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    apiClient.get<Conteudo[]>('/admin/conteudos')
      .then((res) => setConteudos(res.data))
      .catch(() => setError('Não foi possível carregar a lista de conteúdos.'));
  }, []);

  useEffect(() => {
    setSelecionados(new Set(mentoria.materiaisRecomendados.map((m) => m.id)));
  }, [mentoria.materiaisRecomendados]);

  function toggle(id: string) {
    setSelecionados((prev) => {
      const proximo = new Set(prev);
      if (proximo.has(id)) proximo.delete(id);
      else proximo.add(id);
      return proximo;
    });
  }

  const alterado =
    selecionados.size !== mentoria.materiaisRecomendados.length ||
    mentoria.materiaisRecomendados.some((m) => !selecionados.has(m.id));

  async function salvar() {
    setError(null);
    setSalvando(true);
    try {
      await apiClient.patch(`/admin/mentorias/${mentoriaId}/materiais`, { conteudoIds: Array.from(selecionados) });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar os materiais recomendados.'));
    } finally {
      setSalvando(false);
    }
  }

  const conteudosFiltrados = (conteudos ?? []).filter((c) => c.titulo.toLowerCase().includes(busca.trim().toLowerCase()));

  return (
    <Card style={{ padding: 20, marginBottom: 16 }} testId="materiais-card">
      <div className={styles.sectionTitle}>Materiais recomendados</div>
      <p className={styles.muted}>Conteúdos que o mentorado vai ver vinculados a esta mentoria.</p>
      {podeEditar && (
        <input
          className={styles.textInput}
          placeholder="Buscar conteúdo..."
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
        />
      )}
      {!podeEditar && mentoria.materiaisRecomendados.length === 0 && (
        <div className={styles.muted}>Nenhum material recomendado.</div>
      )}
      {!podeEditar ? (
        <div className={styles.materiaisLista}>
          {mentoria.materiaisRecomendados.map((m) => (
            <a key={m.id} href={m.url} target="_blank" rel="noreferrer" className={styles.materiaLink}>
              {m.titulo}
            </a>
          ))}
        </div>
      ) : (
        <div className={styles.checkboxList}>
          {conteudos === null && <div className={styles.muted}>Carregando…</div>}
          {conteudos !== null && conteudosFiltrados.length === 0 && (
            <div className={styles.checkboxEmpty}>Nenhum conteúdo encontrado.</div>
          )}
          {conteudosFiltrados.map((c) => (
            <label key={c.id} className={styles.checkboxItem}>
              <input type="checkbox" checked={selecionados.has(c.id)} onChange={() => toggle(c.id)} />
              {c.titulo}
            </label>
          ))}
        </div>
      )}
      {error && <div className={styles.error}>{error}</div>}
      {podeEditar && (
        <div className={styles.formActions}>
          <button className={styles.actionButton} onClick={salvar} disabled={salvando || !alterado}>
            {salvando ? 'Salvando…' : 'Salvar materiais'}
          </button>
        </div>
      )}
    </Card>
  );
}

function SugestoesCard({ mentoriaId, sugestoes, podeEditar, onSalvo }: {
  mentoriaId: string; sugestoes: SugestaoEncaminhamento[]; podeEditar: boolean; onSalvo: () => void;
}) {
  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Encaminhamentos sugeridos pela IA</div>
      <p className={styles.muted}>
        Revise antes de publicar — só os aceitos viram encaminhamento de verdade (e passam a contar
        no ranking) na publicação da ata.
      </p>
      <div className={styles.sugestoesList}>
        {sugestoes.map((s) => (
          <SugestaoRow key={s.id} mentoriaId={mentoriaId} sugestao={s} podeEditar={podeEditar} onSalvo={onSalvo} />
        ))}
      </div>
    </Card>
  );
}

function SugestaoRow({ mentoriaId, sugestao, podeEditar, onSalvo }: {
  mentoriaId: string; sugestao: SugestaoEncaminhamento; podeEditar: boolean; onSalvo: () => void;
}) {
  const [titulo, setTitulo] = useState(sugestao.titulo);
  const [peso, setPeso] = useState(sugestao.pesoSugerido);
  const [aceito, setAceito] = useState(sugestao.aceito);
  const [salvando, setSalvando] = useState(false);

  const alterado = titulo !== sugestao.titulo || peso !== sugestao.pesoSugerido || aceito !== sugestao.aceito;

  async function salvar() {
    setSalvando(true);
    try {
      await apiClient.patch(`/admin/mentorias/${mentoriaId}/ata/sugestoes/${sugestao.id}`, { titulo, pesoSugerido: peso, aceito });
      onSalvo();
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className={styles.sugestaoRow}>
      <input
        type="checkbox"
        checked={aceito}
        onChange={(e) => setAceito(e.target.checked)}
        disabled={!podeEditar}
      />
      <input
        className={styles.sugestaoTitulo}
        data-testid={`sugestao-titulo-${sugestao.id}`}
        value={titulo}
        onChange={(e) => setTitulo(e.target.value)}
        disabled={!podeEditar}
      />
      <select className={styles.sugestaoPeso} value={peso} onChange={(e) => setPeso(Number(e.target.value))} disabled={!podeEditar}>
        <option value={1}>Peso 1</option>
        <option value={2}>Peso 2</option>
      </select>
      {podeEditar && alterado && (
        <button className={styles.actionButton} onClick={salvar} disabled={salvando}>
          {salvando ? 'Salvando…' : 'Salvar'}
        </button>
      )}
    </div>
  );
}

function PublicarButton({ mentoriaId, desabilitado, onPublicado }: {
  mentoriaId: string; desabilitado: boolean; onPublicado: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [publicando, setPublicando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);

  async function publicar() {
    setError(null);
    setPublicando(true);
    try {
      await apiClient.post(`/admin/mentorias/${mentoriaId}/ata/publicar`);
      onPublicado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível publicar a ata.'));
    } finally {
      setPublicando(false);
      setConfirmando(false);
    }
  }

  return (
    <div>
      {error && <div className={styles.error}>{error}</div>}
      <div className={styles.formActions}>
        <button className={styles.publicarButton} onClick={() => setConfirmando(true)} disabled={desabilitado || publicando}>
          {publicando ? 'Publicando…' : 'Publicar ata'}
        </button>
      </div>
      {confirmando && (
        <ConfirmDialog
          title="Publicar ata?"
          message="O mentorado vai poder ver o resumo da mentoria e os encaminhamentos aceitos viram tarefas de verdade. Revise as sugestões antes de confirmar — depois de publicada, a ata não pode voltar a rascunho."
          confirmLabel="Sim, publicar"
          cancelLabel="Revisar de novo"
          danger={false}
          submitting={publicando}
          onConfirm={publicar}
          onCancel={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}
