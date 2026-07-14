import { type FormEvent, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Ata, StatusProcessamentoAta, SugestaoEncaminhamento } from '../../shared/lib/types';
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
  const [error, setError] = useState<string | null>(null);
  const intervalRef = useRef<number | undefined>(undefined);

  const carregar = () => {
    if (!mentoriaId) return;
    apiClient.get<Ata>(`/admin/mentorias/${mentoriaId}/ata`)
      .then((res) => setAta(res.data))
      .catch(() => setError('Não foi possível carregar a ata.'));
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
  if (!ata) return <div className={styles.loading}>Carregando…</div>;

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

      <ResumoCard mentoriaId={mentoriaId} ata={ata} onSalvo={carregar} />

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

function UploadAudioForm({ mentoriaId, desabilitado, onEnviado }: {
  mentoriaId: string; desabilitado: boolean; onEnviado: () => void;
}) {
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function handleSubmit(e: FormEvent) {
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

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Áudio da mentoria</div>
      <p className={styles.muted}>
        Suba a gravação pra gerar automaticamente a transcrição e um rascunho de resumo (revisão
        humana obrigatória antes de publicar).
      </p>
      <form onSubmit={handleSubmit} className={styles.uploadForm}>
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
      {error && <div className={styles.error}>{error}</div>}
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
