import { type FormEvent, useEffect, useState } from 'react';
import { isAxiosError } from 'axios';
import { apiClient } from '../../shared/lib/apiClient';
import { useAuth } from '../auth/AuthContext';
import { Card } from '../../shared/components/Card';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type {
  DiagnosticoInicial,
  EstadoImplementacao,
  ImportMentoradoDiretoResultResponse,
  Lead,
  MentoradoAdmin,
  MentoradoCriado,
  Plano,
  RespostaSimNao,
  StatusMentorado,
  TipoContrato,
} from '../../shared/lib/types';
import styles from './MentoradosListaPage.module.css';

const COLUMNS = '1.4fr 1.6fr 1.2fr 1fr 1fr 1.6fr';

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito',
  BASICO: 'Básico',
  ESSENCIAL: 'Essencial',
  PROFISSIONAL: 'Profissional',
};

// M23 (change request pós-MVP, 17/07/2026) — TipoContrato é aditivo, não substitui Plano (ver
// ROADMAP.md § Blueprint M23). /direto e /dados-contrato exigem Modulo.COMERCIAL (achado do
// revisor-seguranca: CNPJ/sócios/valor de contrato não são dado de Gestão de Performance).
const TIPO_CONTRATO_LABEL: Record<TipoContrato, string> = {
  MENTORIA_CONTINUA: 'Mentoria Contínua',
  MENTORIA_INDIVIDUAL: 'Mentoria Individual',
  CONSULTORIA: 'Consultoria',
};

const ESTADO_IMPLEMENTACAO_LABEL: Record<EstadoImplementacao, string> = {
  SIM: 'Sim',
  NAO: 'Não',
  EM_CONSTRUCAO: 'Em construção',
};

const RESPOSTA_SIM_NAO_LABEL: Record<RespostaSimNao, string> = {
  SIM: 'Sim',
  NAO: 'Não',
};

const STATUS_LABEL: Record<StatusMentorado, { label: string; bg: string; color: string }> = {
  ATIVO: { label: 'Ativo', bg: 'var(--success-bg)', color: 'var(--success)' },
  INATIVO: { label: 'Inativo', bg: 'var(--line)', color: 'var(--text-soft)' },
};

export function MentoradosListaPage() {
  const { user } = useAuth();
  // M23 — /direto e /dados-contrato exigem Modulo.COMERCIAL (achado do revisor-seguranca); a
  // área Gestão de Performance (que enxerga esta tela via Modulo.MENTORADOS) não vê esses botões.
  const podeVerContrato = user?.modulosPermitidos.includes('COMERCIAL') ?? false;

  const [plano, setPlano] = useState<Plano | ''>('');
  const [status, setStatus] = useState<StatusMentorado | ''>('');
  const [busca, setBusca] = useState('');
  const [mentorados, setMentorados] = useState<MentoradoAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState<MentoradoAdmin | null>(null);
  const [criando, setCriando] = useState(false);
  const [criandoDireto, setCriandoDireto] = useState(false);
  const [criado, setCriado] = useState<MentoradoCriado | null>(null);
  const [importandoDireto, setImportandoDireto] = useState(false);
  const [resultadoImportDireto, setResultadoImportDireto] = useState<ImportMentoradoDiretoResultResponse | null>(null);

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
        <div className={styles.acoes}>
          {podeVerContrato && (
            <button className={styles.newButton} onClick={() => setCriandoDireto(true)}>
              <span style={{ fontSize: 16 }}>+</span>Criar mentorado direto
            </button>
          )}
          {podeVerContrato && (
            <button className={styles.newButton} onClick={() => setImportandoDireto(true)}>
              <span style={{ fontSize: 16 }}>+</span>Importar mentorados em massa
            </button>
          )}
          <button className={styles.newButton} onClick={() => setCriando(true)}>
            <span style={{ fontSize: 16 }}>+</span>Criar a partir de um lead
          </button>
        </div>
      </div>

      <div className={styles.csvRow}>
        <CsvImportExport
          exportUrl="/admin/mentorados/export"
          exportParams={{ plano: plano || undefined, status: status || undefined, busca: busca || undefined }}
          exportFilename="mentorados.csv"
          importUrl="/admin/mentorados/import"
          onImportado={carregar}
          labelPrefix="Mentorados"
        />
      </div>

      {criando && (
        <CriarMentoradoForm
          onCriado={(res) => { setCriando(false); setCriado(res); carregar(); }}
          onCancelar={() => setCriando(false)}
        />
      )}

      {criandoDireto && (
        <CriarMentoradoDiretoForm
          onCriado={(res) => { setCriandoDireto(false); setCriado(res); carregar(); }}
          onCancelar={() => setCriandoDireto(false)}
        />
      )}

      {importandoDireto && (
        <ImportarMentoradosDiretoForm
          onImportado={(res) => { setImportandoDireto(false); setResultadoImportDireto(res); carregar(); }}
          onCancelar={() => setImportandoDireto(false)}
        />
      )}

      {resultadoImportDireto && (
        <Card style={{ padding: 20, marginBottom: 16, borderColor: 'var(--gold)' }}>
          <div className={styles.formTitle}>{resultadoImportDireto.importados} mentorado(s) importado(s)</div>
          <p className={styles.muted}>
            Ainda não há envio automático de e-mail — repasse cada senha temporária manualmente. Elas não
            podem ser recuperadas depois de fechar esta tela.
          </p>
          <div className={styles.credenciais}>
            {resultadoImportDireto.criados.map((c) => (
              <div key={c.id}>
                <strong>{c.nome}</strong> — {c.email} — <code>{c.senhaTemporaria}</code>
              </div>
            ))}
          </div>
          <div className={styles.formActions}>
            <button className={styles.actionButton} onClick={() => setResultadoImportDireto(null)}>Entendi</button>
          </div>
        </Card>
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
          podeVerContrato={podeVerContrato}
          onSalvo={() => { setEditando(null); carregar(); }}
          onAtualizarLista={carregar}
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
                  <ToggleStatusButton mentoradoId={m.id} nome={m.nome} acao="desativar" label="Desativar" onFeito={carregar} />
                ) : (
                  <ToggleStatusButton mentoradoId={m.id} nome={m.nome} acao="ativar" label="Ativar" onFeito={carregar} />
                )}
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>
    </div>
  );
}

function ToggleStatusButton({ mentoradoId, nome, acao, label, onFeito }: {
  mentoradoId: string; nome: string; acao: 'ativar' | 'desativar'; label: string; onFeito: () => void;
}) {
  const [submitting, setSubmitting] = useState(false);
  const [confirmando, setConfirmando] = useState(false);

  async function handleClick() {
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/mentorados/${mentoradoId}/${acao}`);
      onFeito();
    } finally {
      setSubmitting(false);
      setConfirmando(false);
    }
  }

  return (
    <>
      <button className={styles.actionButtonDanger} onClick={() => (acao === 'desativar' ? setConfirmando(true) : handleClick())} disabled={submitting}>
        {label}
      </button>
      {confirmando && (
        <ConfirmDialog
          title="Desativar mentorado?"
          message={`${nome} perderá o acesso à plataforma até ser reativado. Essa ação pode ser revertida depois clicando em "Ativar".`}
          confirmLabel="Confirmar desativação"
          cancelLabel="Voltar"
          submitting={submitting}
          onConfirm={handleClick}
          onCancel={() => setConfirmando(false)}
        />
      )}
    </>
  );
}

function EditarMentoradoForm({ mentorado, podeVerContrato, onSalvo, onAtualizarLista, onCancelar }: {
  mentorado: MentoradoAdmin; podeVerContrato: boolean; onSalvo: () => void; onAtualizarLista: () => void; onCancelar: () => void;
}) {
  const [nome, setNome] = useState(mentorado.nome);
  const [negocio, setNegocio] = useState(mentorado.negocio ?? '');
  const [plano, setPlano] = useState<Plano>(mentorado.plano);
  const [vencimentoPlano, setVencimentoPlano] = useState(mentorado.vencimentoPlano ?? '');
  const [telefone, setTelefone] = useState(mentorado.telefone ?? '');
  const [bio, setBio] = useState(mentorado.bio ?? '');
  const [fotoUrl, setFotoUrl] = useState(mentorado.fotoUrl ?? '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.put(`/admin/mentorados/${mentorado.id}`, {
        nome, negocio: negocio || null, plano, vencimentoPlano: vencimentoPlano || null,
        telefone: telefone || null,
        bio: bio || null,
        fotoUrl: fotoUrl || null,
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
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Telefone
            <input className={styles.textInput} value={telefone} onChange={(e) => setTelefone(e.target.value)} placeholder="(11) 90000-0000" />
          </label>
          <label className={styles.formField} style={{ flex: 2 }}>
            Foto (URL)
            <input className={styles.textInput} value={fotoUrl} onChange={(e) => setFotoUrl(e.target.value)} placeholder="https://..." />
          </label>
        </div>
        <label className={styles.formField}>
          Bio
          <textarea className={styles.textarea} value={bio} onChange={(e) => setBio(e.target.value)} rows={3} />
        </label>
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
      {podeVerContrato && <DadosContratoSection mentorado={mentorado} onSalvo={onAtualizarLista} />}
      <DiagnosticoInicialSection mentoradoId={mentorado.id} />
    </Card>
  );
}

// M23 — CNPJ/sócios/valor de contrato só aparece pra quem tem Modulo.COMERCIAL (achado do
// revisor-seguranca). Seção própria, com o próprio botão salvar — não mistura na mutation do
// perfil (PUT /{id}), que é um endpoint/RBAC diferente (PATCH .../dados-contrato).
function DadosContratoSection({ mentorado, onSalvo }: { mentorado: MentoradoAdmin; onSalvo: () => void }) {
  const [nomeFantasia, setNomeFantasia] = useState(mentorado.nomeFantasia ?? '');
  const [cnpj, setCnpj] = useState(mentorado.cnpj ?? '');
  const [socios, setSocios] = useState(mentorado.socios ?? '');
  const [tipoContrato, setTipoContrato] = useState<TipoContrato | ''>(mentorado.tipoContrato ?? '');
  const [valorContrato, setValorContrato] = useState(mentorado.valorContrato != null ? String(mentorado.valorContrato) : '');
  const [dataFechamentoContrato, setDataFechamentoContrato] = useState(mentorado.dataFechamentoContrato ?? '');
  const [documentoContratoUrl, setDocumentoContratoUrl] = useState(mentorado.documentoContratoUrl);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [salvo, setSalvo] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSalvo(false);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/mentorados/${mentorado.id}/dados-contrato`, {
        nomeFantasia: nomeFantasia || null,
        cnpj: cnpj || null,
        socios: socios || null,
        tipoContrato: tipoContrato || null,
        valorContrato: valorContrato ? Number(valorContrato) : null,
        dataFechamentoContrato: dataFechamentoContrato || null,
      });
      setSalvo(true);
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar os dados de contrato. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form} style={{ marginTop: 20, paddingTop: 20, borderTop: '1px solid var(--line)' }}>
      <div className={styles.formTitle}>Dados de contrato</div>
      <div className={styles.formRow}>
        <label className={styles.formField} style={{ flex: 2 }}>
          Nome fantasia
          <input className={styles.textInput} value={nomeFantasia} onChange={(e) => setNomeFantasia(e.target.value)} />
        </label>
        <label className={styles.formField}>
          CNPJ
          <input className={styles.textInput} value={cnpj} onChange={(e) => setCnpj(e.target.value)} placeholder="00.000.000/0000-00" />
        </label>
        <label className={styles.formField}>
          Tipo de contrato
          <select className={styles.select} value={tipoContrato} onChange={(e) => setTipoContrato(e.target.value as TipoContrato | '')}>
            <option value="">Selecione…</option>
            {(Object.keys(TIPO_CONTRATO_LABEL) as TipoContrato[]).map((t) => (
              <option key={t} value={t}>{TIPO_CONTRATO_LABEL[t]}</option>
            ))}
          </select>
        </label>
      </div>
      <div className={styles.formRow}>
        <label className={styles.formField} style={{ flex: 2 }}>
          Sócios
          <input className={styles.textInput} value={socios} onChange={(e) => setSocios(e.target.value)} placeholder="Nome 1; Nome 2" />
        </label>
        <label className={styles.formField}>
          Valor do contrato (R$)
          <input className={styles.textInput} type="number" min="0" step="0.01" value={valorContrato} onChange={(e) => setValorContrato(e.target.value)} />
        </label>
        <label className={styles.formField}>
          Data de fechamento
          <input className={styles.textInput} type="date" value={dataFechamentoContrato} onChange={(e) => setDataFechamentoContrato(e.target.value)} />
        </label>
      </div>
      {mentorado.vencimentoContrato && (
        <div className={styles.muted}>Vencimento calculado: {mentorado.vencimentoContrato}</div>
      )}
      {error && <div className={styles.error}>{error}</div>}
      <div className={styles.formActions}>
        {salvo && !submitting && <span className={styles.muted}>Salvo.</span>}
        <button type="submit" className={styles.actionButton} disabled={submitting}>
          {submitting ? 'Salvando…' : 'Salvar dados de contrato'}
        </button>
      </div>
      <DocumentoContratoUpload
        mentoradoId={mentorado.id}
        documentoContratoUrl={documentoContratoUrl}
        onEnviado={setDocumentoContratoUrl}
      />
    </form>
  );
}

// M23 — upload/download do PDF do contrato assinado, separado do submit dos campos de texto
// (endpoint diferente, multipart/form-data — ver ContratoDocumentoStorageService no backend).
function DocumentoContratoUpload({ mentoradoId, documentoContratoUrl, onEnviado }: {
  mentoradoId: string; documentoContratoUrl: string | null; onEnviado: (url: string) => void;
}) {
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [baixando, setBaixando] = useState(false);

  async function handleEnviar() {
    if (!arquivo) return;
    setError(null);
    setEnviando(true);
    try {
      const formData = new FormData();
      formData.append('arquivo', arquivo);
      const res = await apiClient.post<MentoradoAdmin>(`/admin/mentorados/${mentoradoId}/documento-contrato`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setArquivo(null);
      onEnviado(res.data.documentoContratoUrl ?? '');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível enviar o documento do contrato.'));
    } finally {
      setEnviando(false);
    }
  }

  async function handleBaixar() {
    setError(null);
    setBaixando(true);
    try {
      const res = await apiClient.get(`/admin/mentorados/${mentoradoId}/documento-contrato`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data as Blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'contrato.pdf';
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível baixar o documento do contrato.'));
    } finally {
      setBaixando(false);
    }
  }

  return (
    <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 8 }}>
      <label className={styles.formField}>
        Documento do contrato (PDF)
        <input type="file" accept="application/pdf" onChange={(e) => setArquivo(e.target.files?.[0] ?? null)} disabled={enviando} />
      </label>
      <div className={styles.formActions} style={{ justifyContent: 'flex-start' }}>
        <button type="button" className={styles.actionButton} onClick={handleEnviar} disabled={!arquivo || enviando}>
          {enviando ? 'Enviando…' : 'Enviar documento'}
        </button>
        {documentoContratoUrl && (
          <button type="button" className={styles.cancelButton} onClick={handleBaixar} disabled={baixando}>
            {baixando ? 'Baixando…' : 'Baixar contrato atual'}
          </button>
        )}
      </div>
      {error && <div className={styles.error}>{error}</div>}
    </div>
  );
}

// M23 — Diagnóstico Inicial (feito pela Leia antes da 1ª reunião com o Mateus). Busca o valor
// atual ao montar (GET .../diagnostico-inicial) — é preenchido incrementalmente, então precisa
// carregar o que já existe, não sempre partir de um formulário em branco.
function DiagnosticoInicialSection({ mentoradoId }: { mentoradoId: string }) {
  const [diagnostico, setDiagnostico] = useState<DiagnosticoInicial | null>(null);
  const [faturamentoAnual, setFaturamentoAnual] = useState('');
  const [quantidadeColaboradores, setQuantidadeColaboradores] = useState('');
  const [empresaRegularizada, setEmpresaRegularizada] = useState('');
  const [quantidadeLojas, setQuantidadeLojas] = useState('');
  const [cmvDefinido, setCmvDefinido] = useState<RespostaSimNao | ''>('');
  const [cmvDetalhe, setCmvDetalhe] = useState('');
  const [tempoMedioAtendimento, setTempoMedioAtendimento] = useState('');
  const [culturaConstruida, setCulturaConstruida] = useState<EstadoImplementacao>('NAO');
  const [processosDesenhados, setProcessosDesenhados] = useState<EstadoImplementacao>('NAO');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [salvo, setSalvo] = useState(false);

  useEffect(() => {
    apiClient.get<DiagnosticoInicial>(`/admin/mentorados/${mentoradoId}/diagnostico-inicial`)
      .then((res) => {
        const d = res.data;
        setDiagnostico(d);
        setFaturamentoAnual(d.faturamentoAnual != null ? String(d.faturamentoAnual) : '');
        setQuantidadeColaboradores(d.quantidadeColaboradores != null ? String(d.quantidadeColaboradores) : '');
        setEmpresaRegularizada(d.empresaRegularizada == null ? '' : String(d.empresaRegularizada));
        setQuantidadeLojas(d.quantidadeLojas != null ? String(d.quantidadeLojas) : '');
        setCmvDefinido(d.cmvDefinido ?? '');
        setCmvDetalhe(d.cmvDetalhe ?? '');
        setTempoMedioAtendimento(d.tempoMedioAtendimento ?? '');
        setCulturaConstruida(d.culturaConstruida ?? 'NAO');
        setProcessosDesenhados(d.processosDesenhados ?? 'NAO');
      })
      .catch(() => setError('Não foi possível carregar o Diagnóstico Inicial.'));
  }, [mentoradoId]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSalvo(false);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/mentorados/${mentoradoId}/diagnostico-inicial`, {
        faturamentoAnual: faturamentoAnual ? Number(faturamentoAnual) : null,
        quantidadeColaboradores: quantidadeColaboradores ? Number(quantidadeColaboradores) : null,
        empresaRegularizada: empresaRegularizada === '' ? null : empresaRegularizada === 'true',
        quantidadeLojas: quantidadeLojas ? Number(quantidadeLojas) : null,
        cmvDefinido: cmvDefinido || null,
        cmvDetalhe: cmvDetalhe || null,
        tempoMedioAtendimento: tempoMedioAtendimento || null,
        culturaConstruida,
        processosDesenhados,
      });
      setSalvo(true);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o Diagnóstico Inicial. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (diagnostico === null && !error) {
    return <div className={styles.loading}>Carregando Diagnóstico Inicial…</div>;
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form} style={{ marginTop: 20, paddingTop: 20, borderTop: '1px solid var(--line)' }}>
      <div className={styles.formTitle}>Diagnóstico Inicial</div>
      <div className={styles.formRow}>
        <label className={styles.formField}>
          Faturamento anual (R$)
          <input className={styles.textInput} type="number" min="0" step="0.01" value={faturamentoAnual} onChange={(e) => setFaturamentoAnual(e.target.value)} />
        </label>
        <label className={styles.formField}>
          Nº de colaboradores
          <input className={styles.textInput} type="number" min="0" value={quantidadeColaboradores} onChange={(e) => setQuantidadeColaboradores(e.target.value)} />
        </label>
        <label className={styles.formField}>
          Nº de lojas
          <input className={styles.textInput} type="number" min="0" value={quantidadeLojas} onChange={(e) => setQuantidadeLojas(e.target.value)} />
        </label>
        <label className={styles.formField}>
          Empresa regularizada?
          <select className={styles.select} value={empresaRegularizada} onChange={(e) => setEmpresaRegularizada(e.target.value)}>
            <option value="">Não perguntado</option>
            <option value="true">Sim</option>
            <option value="false">Não</option>
          </select>
        </label>
      </div>
      <div className={styles.formRow}>
        <label className={styles.formField}>
          CMV definido?
          <select className={styles.select} value={cmvDefinido} onChange={(e) => setCmvDefinido(e.target.value as RespostaSimNao | '')}>
            <option value="">Não perguntado</option>
            {(Object.keys(RESPOSTA_SIM_NAO_LABEL) as RespostaSimNao[]).map((r) => (
              <option key={r} value={r}>{RESPOSTA_SIM_NAO_LABEL[r]}</option>
            ))}
          </select>
        </label>
        <label className={styles.formField} style={{ flex: 2 }}>
          Qual (se sim)
          <input className={styles.textInput} value={cmvDetalhe} onChange={(e) => setCmvDetalhe(e.target.value)} />
        </label>
        <label className={styles.formField} style={{ flex: 2 }}>
          Tempo médio de atendimento
          <input className={styles.textInput} value={tempoMedioAtendimento} onChange={(e) => setTempoMedioAtendimento(e.target.value)} placeholder="5 a 10 minutos" />
        </label>
      </div>
      <div className={styles.formRow}>
        <label className={styles.formField}>
          Cultura construída?
          <select className={styles.select} value={culturaConstruida} onChange={(e) => setCulturaConstruida(e.target.value as EstadoImplementacao)}>
            {(Object.keys(ESTADO_IMPLEMENTACAO_LABEL) as EstadoImplementacao[]).map((v) => (
              <option key={v} value={v}>{ESTADO_IMPLEMENTACAO_LABEL[v]}</option>
            ))}
          </select>
        </label>
        <label className={styles.formField}>
          Processos desenhados?
          <select className={styles.select} value={processosDesenhados} onChange={(e) => setProcessosDesenhados(e.target.value as EstadoImplementacao)}>
            {(Object.keys(ESTADO_IMPLEMENTACAO_LABEL) as EstadoImplementacao[]).map((v) => (
              <option key={v} value={v}>{ESTADO_IMPLEMENTACAO_LABEL[v]}</option>
            ))}
          </select>
        </label>
      </div>
      {error && <div className={styles.error}>{error}</div>}
      <div className={styles.formActions}>
        {salvo && !submitting && <span className={styles.muted}>Salvo.</span>}
        <button type="submit" className={styles.actionButton} disabled={submitting}>
          {submitting ? 'Salvando…' : 'Salvar Diagnóstico Inicial'}
        </button>
      </div>
    </form>
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
    apiClient.get<Lead[]>('/admin/comercial/leads', { params: { status: 'FECHADO' } })
      .then((res) => setLeads(res.data))
      .catch(() => setError('Não foi possível carregar a lista de leads fechados.'));
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

// M23 — "criar mentorado direto" (pedido explícito do cliente: cria um Lead já FECHADO
// automaticamente, sem exigir um Lead pré-existente no funil). Só COMERCIAL/ADMIN veem o botão
// que abre este form (achado do revisor-seguranca).
function CriarMentoradoDiretoForm({ onCriado, onCancelar }: {
  onCriado: (res: MentoradoCriado) => void; onCancelar: () => void;
}) {
  const [email, setEmail] = useState('');
  const [nome, setNome] = useState('');
  const [negocio, setNegocio] = useState('');
  const [telefone, setTelefone] = useState('');
  const [tipoContrato, setTipoContrato] = useState<TipoContrato | ''>('');
  const [valorContrato, setValorContrato] = useState('');
  const [dataFechamentoContrato, setDataFechamentoContrato] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const res = await apiClient.post<MentoradoCriado>('/admin/mentorados/direto', {
        email, nome, negocio: negocio || null, telefone: telefone || null,
        tipoContrato: tipoContrato || null,
        valorContrato: valorContrato ? Number(valorContrato) : null,
        dataFechamentoContrato: dataFechamentoContrato || null,
      });
      onCriado(res.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar o mentorado. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Criar mentorado direto</div>
      <p className={styles.muted} style={{ marginTop: -8, marginBottom: 4 }}>
        Pra quando a venda não passou pelo funil comercial de leads (ex.: parceria fechada fora do
        funil, migração de cliente antigo). Cria automaticamente um lead já fechado por trás.
      </p>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Nome
            <input className={styles.textInput} value={nome} onChange={(e) => setNome(e.target.value)} required />
          </label>
          <label className={styles.formField} style={{ flex: 2 }}>
            E-mail
            <input className={styles.textInput} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Telefone
            <input className={styles.textInput} value={telefone} onChange={(e) => setTelefone(e.target.value)} placeholder="(11) 90000-0000" />
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Negócio
            <input className={styles.textInput} value={negocio} onChange={(e) => setNegocio(e.target.value)} />
          </label>
          <label className={styles.formField}>
            Tipo de contrato
            <select className={styles.select} value={tipoContrato} onChange={(e) => setTipoContrato(e.target.value as TipoContrato | '')} required>
              <option value="">Selecione…</option>
              {(Object.keys(TIPO_CONTRATO_LABEL) as TipoContrato[]).map((t) => (
                <option key={t} value={t}>{TIPO_CONTRATO_LABEL[t]}</option>
              ))}
            </select>
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Valor do contrato (R$)
            <input className={styles.textInput} type="number" min="0" step="0.01" value={valorContrato} onChange={(e) => setValorContrato(e.target.value)} />
          </label>
          <label className={styles.formField}>
            Data de fechamento
            <input className={styles.textInput} type="date" value={dataFechamentoContrato} onChange={(e) => setDataFechamentoContrato(e.target.value)} />
          </label>
        </div>
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting || !tipoContrato}>
            {submitting ? 'Criando…' : 'Criar mentorado'}
          </button>
        </div>
      </form>
    </Card>
  );
}

const COLUNAS_IMPORT_DIRETO = [
  'email', 'nome', 'negocio', 'nomeFantasia', 'cnpj', 'socios', 'telefone', 'tipoContrato', 'valorContrato',
  'dataFechamentoContrato', 'faturamentoAnual', 'quantidadeColaboradores', 'empresaRegularizada',
  'quantidadeLojas', 'cmvDefinido', 'cmvDetalhe', 'tempoMedioAtendimento', 'culturaConstruida', 'processosDesenhados',
];

// M23 item 4 (bulk-CREATE, 19/07/2026) — import CSV que CRIA mentorados novos em massa, pensado
// pra migrar de uma vez as empresas reais que hoje só existem no Notion. Só COMERCIAL/ADMIN veem o
// botão que abre este form (mesmo gate de "Criar mentorado direto" — cria credencial + carrega
// CNPJ/sócios/valor de contrato).
function ImportarMentoradosDiretoForm({ onImportado, onCancelar }: {
  onImportado: (res: ImportMentoradoDiretoResultResponse) => void; onCancelar: () => void;
}) {
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [erros, setErros] = useState<ImportMentoradoDiretoResultResponse['erros'] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!arquivo) return;
    setError(null);
    setErros(null);
    setSubmitting(true);
    const form = new FormData();
    form.append('arquivo', arquivo);
    try {
      const res = await apiClient.post<ImportMentoradoDiretoResultResponse>('/admin/mentorados/importar-em-massa', form);
      onImportado(res.data);
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 422 && Array.isArray(err.response.data?.erros)) {
        setErros((err.response.data as ImportMentoradoDiretoResultResponse).erros);
      } else {
        setError(getApiErrorMessage(err, 'Não foi possível importar o arquivo.'));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Importar mentorados em massa</div>
      <p className={styles.muted} style={{ marginTop: -8, marginBottom: 4 }}>
        Cria um mentorado (com Lead já fechado + credencial de login) por linha do CSV. Tudo-ou-nada:
        se alguma linha tiver erro, nada é criado. Colunas esperadas (só email/nome/tipoContrato são
        obrigatórias): <code>{COLUNAS_IMPORT_DIRETO.join(', ')}</code>.
      </p>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.formField}>
          Arquivo CSV
          <input
            className={styles.textInput}
            type="file"
            accept=".csv"
            onChange={(e) => setArquivo(e.target.files?.[0] ?? null)}
            required
          />
        </label>

        {error && <div className={styles.error}>{error}</div>}
        {erros && erros.length > 0 && (
          <div className={styles.error}>
            <div>Nenhuma linha foi importada — corrija o arquivo e reenvie:</div>
            <ul>
              {erros.map((e) => (
                <li key={e.linha}>Linha {e.linha}: {e.motivo}</li>
              ))}
            </ul>
          </div>
        )}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting || !arquivo}>
            {submitting ? 'Importando…' : 'Importar'}
          </button>
        </div>
      </form>
    </Card>
  );
}
