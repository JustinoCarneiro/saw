import { type FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { useAuth } from '../auth/AuthContext';
import { Avatar } from '../../shared/components/Avatar';
import { Card } from '../../shared/components/Card';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { Pill, StatusPill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type {
  CotaEventos,
  DiagnosticoInicial,
  EstadoImplementacao,
  Evento,
  EventoInscricaoAdmin,
  Mentoria,
  MentoradoAdmin,
  MentoradoConsolidado,
  NivelEngajamento,
  RespostaSimNao,
  RiscoChurn,
  StatusInscricao,
  TipoContrato,
} from '../../shared/lib/types';
import styles from './MentoradoDetalhePage.module.css';

// M28 (change request, 21/07/2026, "página dedicada de mentorado") — a maior parte destas
// constantes/formulários migrou 1:1 de MentoradosListaPage.tsx (EditarMentoradoForm + suas 4
// sub-seções, que viviam expandidas inline na mesma tela da lista). Ver ROADMAP.md § M28.
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

const NIVEL_ENGAJAMENTO_LABEL: Record<NivelEngajamento, string> = {
  ALTO: 'Alto',
  MEDIO: 'Médio',
  BAIXO: 'Baixo',
};

const RISCO_CHURN_LABEL: Record<RiscoChurn, string> = {
  NAO: 'Não',
  ATENCAO: 'Atenção',
  ALTO: 'Alto',
};

const NIVEL_ENGAJAMENTO_TOKEN: Record<NivelEngajamento, { label: string; bg: string; color: string }> = {
  ALTO: { label: 'Alto', bg: 'var(--success-bg)', color: 'var(--success)' },
  MEDIO: { label: 'Médio', bg: 'var(--warning-bg)', color: 'var(--warning)' },
  BAIXO: { label: 'Baixo', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

const RISCO_CHURN_TOKEN: Record<RiscoChurn, { label: string; bg: string; color: string }> = {
  NAO: { label: 'Sem risco', bg: 'var(--success-bg)', color: 'var(--success)' },
  ATENCAO: { label: 'Atenção', bg: 'var(--warning-bg)', color: 'var(--warning)' },
  ALTO: { label: 'Alto risco', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

function formatPct(pct: number): string {
  const sign = pct > 0 ? '+' : '';
  return `${sign}${pct}%`;
}

export function MentoradoDetalhePage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const podeVerContrato = user?.modulosPermitidos.includes('COMERCIAL') ?? false;

  const [mentorado, setMentorado] = useState<MentoradoAdmin | null>(null);
  const [metricas, setMetricas] = useState<MentoradoConsolidado | null>(null);
  const [error, setError] = useState<string | null>(null);

  const carregar = () => {
    if (!id) return;
    apiClient.get<MentoradoAdmin>(`/admin/mentorados/${id}`)
      .then((res) => setMentorado(res.data))
      .catch(() => setError('Não foi possível carregar o mentorado.'));
  };

  useEffect(carregar, [id]);

  useEffect(() => {
    if (!id) return;
    // Reaproveita o Painel Consolidado (mesmo padrão de busca client-side já usado em
    // ConsolidatedPage — escala do MVP, 10-15 mentorados, não justifica um endpoint novo só pra
    // filtrar por id). Métrica é complementar: falha aqui não bloqueia o resto da página.
    apiClient.get<MentoradoConsolidado[]>('/admin/consolidated/mentorados')
      .then((res) => setMetricas(res.data.find((m) => m.id === id) ?? null))
      .catch(() => undefined);
  }, [id]);

  if (!id) return null;
  if (error) return <div className={styles.error}>{error}</div>;
  if (!mentorado) return <div className={styles.loading}>Carregando…</div>;

  return (
    <div className={styles.page}>
      <button className={styles.backLink} onClick={() => navigate('/admin/mentorados/lista')}>
        <svg
          width="15"
          height="15"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <path d="M19 12H5" />
          <path d="M11 18L5 12L11 6" />
        </svg>
        Mentorados
      </button>

      <div className={styles.header}>
        <Avatar name={mentorado.nome} size={56} />
        <div className={styles.headerText}>
          <h1 className={styles.nome}>{mentorado.nome}</h1>
          <div className={styles.subline}>{mentorado.negocio ?? 'Sem negócio informado'} · {mentorado.email}</div>
        </div>
        <div className={styles.headerRight}>
          <Pill bg="var(--line)" color="var(--text-soft)">
            {mentorado.tipoContrato ? TIPO_CONTRATO_LABEL[mentorado.tipoContrato] : 'Sem tipo de contrato'}
          </Pill>
          <StatusPill status={mentorado.status} />
          <ToggleStatusButton mentorado={mentorado} onFeito={carregar} />
        </div>
      </div>

      {metricas && <MetricasCard metricas={metricas} />}

      <PerfilSection mentorado={mentorado} onSalvo={carregar} />
      {podeVerContrato && <DadosContratoSection mentorado={mentorado} onSalvo={carregar} />}
      <DiagnosticoInicialSection mentoradoId={mentorado.id} />
      <FerramentasObrigatoriasSection mentorado={mentorado} onSalvo={carregar} />
      <AcompanhamentoSection mentorado={mentorado} onSalvo={carregar} />
      <MentoriasSection mentoradoId={mentorado.id} />
      <EventosSection mentorado={mentorado} />
    </div>
  );
}

function ToggleStatusButton({ mentorado, onFeito }: { mentorado: MentoradoAdmin; onFeito: () => void }) {
  const [submitting, setSubmitting] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const acao = mentorado.status === 'ATIVO' ? 'desativar' : 'ativar';

  async function handleClick() {
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/mentorados/${mentorado.id}/${acao}`);
      onFeito();
    } finally {
      setSubmitting(false);
      setConfirmando(false);
    }
  }

  return (
    <>
      <button
        className={styles.actionButtonDanger}
        onClick={() => (acao === 'desativar' ? setConfirmando(true) : handleClick())}
        disabled={submitting}
      >
        {acao === 'desativar' ? 'Desativar' : 'Ativar'}
      </button>
      {confirmando && (
        <ConfirmDialog
          title="Desativar mentorado?"
          message={`${mentorado.nome} perderá o acesso à plataforma até ser reativado. Essa ação pode ser revertida depois clicando em "Ativar".`}
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

// E17 — reaproveita os mesmos campos/cores do Painel Consolidado (ConsolidatedPage), não inventa
// paleta nova. "Foco total" pedido pelo Marcos: essas métricas hoje só existiam numa linha de
// tabela lá; aqui viram o resumo de saúde do mentorado logo no topo da página dele.
function MetricasCard({ metricas }: { metricas: MentoradoConsolidado }) {
  return (
    <div className={styles.metricsGrid}>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Status</div>
        <StatusPill status={metricas.status} />
      </Card>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Progresso</div>
        <div className={styles.metricValue}>{metricas.progressoPct}%</div>
      </Card>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Encaminhamentos</div>
        <div className={styles.metricValue}>{metricas.encaminhamentosCumpridos}/{metricas.encaminhamentosTotal}</div>
      </Card>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Ferramentas obrigatórias</div>
        <div className={styles.metricValue}>{metricas.ferramentasPct}%</div>
      </Card>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Frequência em mentoria</div>
        <div className={styles.metricValue}>{metricas.frequenciaMentoriaPct != null ? `${metricas.frequenciaMentoriaPct}%` : '—'}</div>
      </Card>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Crescimento de faturamento</div>
        <div className={styles.metricValue} style={{ color: metricas.crescimentoFaturamentoPct >= 0 ? 'var(--success)' : 'var(--danger)' }}>
          {formatPct(metricas.crescimentoFaturamentoPct)}
        </div>
      </Card>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Nível de engajamento</div>
        {metricas.nivelEngajamento ? (
          <Pill bg={NIVEL_ENGAJAMENTO_TOKEN[metricas.nivelEngajamento].bg} color={NIVEL_ENGAJAMENTO_TOKEN[metricas.nivelEngajamento].color}>
            {NIVEL_ENGAJAMENTO_TOKEN[metricas.nivelEngajamento].label}
          </Pill>
        ) : (
          <span className={styles.muted}>Não avaliado</span>
        )}
      </Card>
      <Card className={styles.metricCard}>
        <div className={styles.metricLabel}>Risco de churn</div>
        {metricas.riscoChurn ? (
          <Pill bg={RISCO_CHURN_TOKEN[metricas.riscoChurn].bg} color={RISCO_CHURN_TOKEN[metricas.riscoChurn].color}>
            {RISCO_CHURN_TOKEN[metricas.riscoChurn].label}
          </Pill>
        ) : (
          <span className={styles.muted}>Não avaliado</span>
        )}
      </Card>
    </div>
  );
}

function PerfilSection({ mentorado, onSalvo }: { mentorado: MentoradoAdmin; onSalvo: () => void }) {
  const [nome, setNome] = useState(mentorado.nome);
  const [negocio, setNegocio] = useState(mentorado.negocio ?? '');
  const [telefone, setTelefone] = useState(mentorado.telefone ?? '');
  const [bio, setBio] = useState(mentorado.bio ?? '');
  const [fotoUrl, setFotoUrl] = useState(mentorado.fotoUrl ?? '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [salvo, setSalvo] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSalvo(false);
    setSubmitting(true);
    try {
      await apiClient.put(`/admin/mentorados/${mentorado.id}`, {
        nome, negocio: negocio || null,
        telefone: telefone || null,
        bio: bio || null,
        fotoUrl: fotoUrl || null,
      });
      setSalvo(true);
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Perfil</div>
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
          {salvo && !submitting && <span className={styles.muted}>Salvo.</span>}
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
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
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Dados de contrato</div>
      <form onSubmit={handleSubmit} className={styles.form}>
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
      </form>
      <DocumentoContratoUpload
        mentoradoId={mentorado.id}
        documentoContratoUrl={documentoContratoUrl}
        onEnviado={setDocumentoContratoUrl}
      />
    </Card>
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
    return (
      <Card style={{ padding: 20, marginBottom: 16 }}>
        <div className={styles.loading}>Carregando Diagnóstico Inicial…</div>
      </Card>
    );
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Diagnóstico Inicial</div>
      <form onSubmit={handleSubmit} className={styles.form}>
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
    </Card>
  );
}

// E17/M27 (change request pós-MVP, 19/07/2026) — as 4 ferramentas obrigatórias nomeadas do
// ranking (ver ROADMAP.md § "Blueprint (M27)"). Diferente do Diagnóstico Inicial, o valor atual
// já vem no próprio `mentorado` (MentoradoResponse) — não precisa de um GET separado.
function FerramentasObrigatoriasSection({ mentorado, onSalvo }: { mentorado: MentoradoAdmin; onSalvo: () => void }) {
  const [ferramentaDre, setFerramentaDre] = useState<EstadoImplementacao>(mentorado.ferramentaDre);
  const [ferramentaManualCultura, setFerramentaManualCultura] = useState<EstadoImplementacao>(mentorado.ferramentaManualCultura);
  const [ferramentaFichaTecnica, setFerramentaFichaTecnica] = useState<EstadoImplementacao>(mentorado.ferramentaFichaTecnica);
  const [ferramentaManualProcessos, setFerramentaManualProcessos] = useState<EstadoImplementacao>(mentorado.ferramentaManualProcessos);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [salvo, setSalvo] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSalvo(false);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/mentorados/${mentorado.id}/ferramentas-obrigatorias`, {
        ferramentaDre, ferramentaManualCultura, ferramentaFichaTecnica, ferramentaManualProcessos,
      });
      setSalvo(true);
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar as ferramentas obrigatórias. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Ferramentas obrigatórias</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            DRE estruturada
            <select className={styles.select} value={ferramentaDre} onChange={(e) => setFerramentaDre(e.target.value as EstadoImplementacao)}>
              {(Object.keys(ESTADO_IMPLEMENTACAO_LABEL) as EstadoImplementacao[]).map((v) => (
                <option key={v} value={v}>{ESTADO_IMPLEMENTACAO_LABEL[v]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Manual de cultura
            <select className={styles.select} value={ferramentaManualCultura} onChange={(e) => setFerramentaManualCultura(e.target.value as EstadoImplementacao)}>
              {(Object.keys(ESTADO_IMPLEMENTACAO_LABEL) as EstadoImplementacao[]).map((v) => (
                <option key={v} value={v}>{ESTADO_IMPLEMENTACAO_LABEL[v]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Ficha técnica
            <select className={styles.select} value={ferramentaFichaTecnica} onChange={(e) => setFerramentaFichaTecnica(e.target.value as EstadoImplementacao)}>
              {(Object.keys(ESTADO_IMPLEMENTACAO_LABEL) as EstadoImplementacao[]).map((v) => (
                <option key={v} value={v}>{ESTADO_IMPLEMENTACAO_LABEL[v]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Manual de processos
            <select className={styles.select} value={ferramentaManualProcessos} onChange={(e) => setFerramentaManualProcessos(e.target.value as EstadoImplementacao)}>
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
            {submitting ? 'Salvando…' : 'Salvar ferramentas obrigatórias'}
          </button>
        </div>
      </form>
    </Card>
  );
}

// E17/M27 — dois eixos de acompanhamento, preenchidos manualmente pelo mentor/time de sucesso
// (não calculados — ver ROADMAP.md § "Blueprint (M27)"). "Não avaliado" mapeia pra null na
// request (semântica de PATCH no backend: campo null não apaga valor já registrado).
function AcompanhamentoSection({ mentorado, onSalvo }: { mentorado: MentoradoAdmin; onSalvo: () => void }) {
  const [nivelEngajamento, setNivelEngajamento] = useState<NivelEngajamento | ''>(mentorado.nivelEngajamento ?? '');
  const [riscoChurn, setRiscoChurn] = useState<RiscoChurn | ''>(mentorado.riscoChurn ?? '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [salvo, setSalvo] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSalvo(false);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/mentorados/${mentorado.id}/acompanhamento`, {
        nivelEngajamento: nivelEngajamento || null,
        riscoChurn: riscoChurn || null,
      });
      setSalvo(true);
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o acompanhamento. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Acompanhamento</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Nível de engajamento
            <select className={styles.select} value={nivelEngajamento} onChange={(e) => setNivelEngajamento(e.target.value as NivelEngajamento | '')}>
              <option value="">Não avaliado</option>
              {(Object.keys(NIVEL_ENGAJAMENTO_LABEL) as NivelEngajamento[]).map((v) => (
                <option key={v} value={v}>{NIVEL_ENGAJAMENTO_LABEL[v]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Risco de churn
            <select className={styles.select} value={riscoChurn} onChange={(e) => setRiscoChurn(e.target.value as RiscoChurn | '')}>
              <option value="">Não avaliado</option>
              {(Object.keys(RISCO_CHURN_LABEL) as RiscoChurn[]).map((v) => (
                <option key={v} value={v}>{RISCO_CHURN_LABEL[v]}</option>
              ))}
            </select>
          </label>
        </div>
        {mentorado.acompanhamentoAvaliadoEm && (
          <div className={styles.muted}>Última avaliação: {new Date(mentorado.acompanhamentoAvaliadoEm).toLocaleString('pt-BR')}</div>
        )}
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          {salvo && !submitting && <span className={styles.muted}>Salvo.</span>}
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar acompanhamento'}
          </button>
        </div>
      </form>
    </Card>
  );
}

const STATUS_MENTORIA_LABEL: Record<string, { label: string; bg: string; color: string }> = {
  AGENDADA: { label: 'Agendada', bg: 'var(--line)', color: 'var(--text-soft)' },
  CONFIRMADA: { label: 'Confirmada', bg: 'var(--info-bg)', color: 'var(--info)' },
  REALIZADA: { label: 'Realizada', bg: 'var(--success-bg)', color: 'var(--success)' },
  CANCELADA: { label: 'Cancelada', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

const TIPO_MENTORIA_LABEL: Record<string, string> = {
  INDIVIDUAL: 'Individual',
  GRUPO: 'Grupo',
};

function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

// M28 (change request, 21/07/2026, "reorganizar lista de mentorias") — histórico de mentorias
// deste mentorado (individual, consultoria e grupo), que deixou de aparecer misturado na lista
// central (MentoriasAgendaPage passa a mostrar só Grupo por padrão). Reaproveita GET
// /admin/mentorias?mentoradoId= (MentoriaService#listar, M28).
function MentoriasSection({ mentoradoId }: { mentoradoId: string }) {
  const navigate = useNavigate();
  const [mentorias, setMentorias] = useState<Mentoria[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient.get<Mentoria[]>('/admin/mentorias', { params: { mentoradoId } })
      .then((res) => setMentorias(res.data))
      .catch(() => setError('Não foi possível carregar as mentorias.'));
  }, [mentoradoId]);

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Mentorias</div>
      {error && <div className={styles.error}>{error}</div>}
      {!error && mentorias === null && <div className={styles.loading}>Carregando…</div>}
      {mentorias?.length === 0 && <div className={styles.muted}>Nenhuma mentoria registrada ainda.</div>}
      {mentorias && mentorias.length > 0 && (
        <div className={styles.mentoriasList}>
          {mentorias.map((m) => {
            const st = STATUS_MENTORIA_LABEL[m.status];
            return (
              <div key={m.id} className={styles.mentoriaRow}>
                <div className={styles.mentoriaData}>{formatarDataHora(m.dataHora)}</div>
                <div className={styles.mentoriaTipo}>{TIPO_MENTORIA_LABEL[m.tipo] ?? m.tipo}</div>
                <div className={styles.mentoriaMentor}>{m.mentor.nome}</div>
                <Pill bg={st.bg} color={st.color}>{st.label}</Pill>
                {m.status === 'REALIZADA' && (
                  <button
                    className={styles.actionButton}
                    onClick={() => navigate(`/admin/mentorados/mentorias/${m.id}/ata`)}
                  >
                    Ver ata
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </Card>
  );
}

const STATUS_INSCRICAO_LABEL: Record<StatusInscricao, { label: string; bg: string; color: string }> = {
  INSCRITA: { label: 'Inscrito', bg: 'var(--info-bg)', color: 'var(--info)' },
  PARTICIPOU: { label: 'Participou', bg: 'var(--success-bg)', color: 'var(--success)' },
  CANCELADA: { label: 'Cancelada', bg: 'var(--line)', color: 'var(--text-soft)' },
};

function formatarData(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR');
}

// M28 (change request, 21/07/2026, "controle de vagas em evento por mentorado da Contínua") —
// como a área do mentorado está pausada (AREA_MENTORADO_PAUSADA), este é hoje o ÚNICO jeito de
// inscrever alguém num evento: o self-service em EventoMentoradoController é inalcançável na
// prática. A cota de 3 grátis/ano de contrato (Mentoria Contínua) é aplicada pelo backend nos
// dois caminhos (ver EventoMentoradoService#inscreverNucleo) — aqui só exibe o resumo já calculado
// (GET .../eventos/cota), nunca recalcula a janela rolante no cliente.
function EventosSection({ mentorado }: { mentorado: MentoradoAdmin }) {
  const [inscricoes, setInscricoes] = useState<EventoInscricaoAdmin[] | null>(null);
  const [cota, setCota] = useState<CotaEventos | null>(null);
  const [eventosDisponiveis, setEventosDisponiveis] = useState<Evento[] | null>(null);
  const [eventoSelecionado, setEventoSelecionado] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [processando, setProcessando] = useState(false);

  const carregarInscricoesECota = () => {
    apiClient.get<EventoInscricaoAdmin[]>(`/admin/mentorados/${mentorado.id}/eventos/inscricoes`)
      .then((res) => setInscricoes(res.data))
      .catch(() => setError('Não foi possível carregar as inscrições em eventos.'));
    apiClient.get<CotaEventos>(`/admin/mentorados/${mentorado.id}/eventos/cota`)
      .then((res) => setCota(res.data))
      .catch(() => undefined);
  };

  useEffect(carregarInscricoesECota, [mentorado.id]);

  useEffect(() => {
    apiClient.get<Evento[]>('/admin/eventos')
      .then((res) => setEventosDisponiveis(res.data.filter((e) => e.status === 'PROGRAMADO' || e.status === 'AO_VIVO')))
      .catch(() => undefined);
  }, []);

  async function inscrever() {
    if (!eventoSelecionado) return;
    setError(null);
    setProcessando(true);
    try {
      await apiClient.post(`/admin/mentorados/${mentorado.id}/eventos/${eventoSelecionado}/inscricao`);
      setEventoSelecionado('');
      carregarInscricoesECota();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível inscrever o mentorado neste evento.'));
    } finally {
      setProcessando(false);
    }
  }

  async function cancelar(eventoId: string) {
    setError(null);
    setProcessando(true);
    try {
      await apiClient.delete(`/admin/mentorados/${mentorado.id}/eventos/${eventoId}/inscricao`);
      carregarInscricoesECota();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível cancelar a inscrição.'));
    } finally {
      setProcessando(false);
    }
  }

  const inscritosAtivos = new Set((inscricoes ?? []).filter((i) => i.status === 'INSCRITA').map((i) => i.eventoId));
  const opcoesParaInscrever = (eventosDisponiveis ?? []).filter((e) => !inscritosAtivos.has(e.id));

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.sectionTitle}>Eventos</div>
      {cota?.aplicavel && (
        <p className={styles.muted}>
          {cota.usadas}/{cota.limite} eventos grátis usados neste ciclo de contrato (Mentoria Contínua)
          {cota.fimCiclo && ` — renova em ${formatarData(cota.fimCiclo)}`}.
        </p>
      )}

      <div className={styles.formRow}>
        <label className={styles.formField}>
          Inscrever em
          <select
            className={styles.select}
            value={eventoSelecionado}
            onChange={(e) => setEventoSelecionado(e.target.value)}
            disabled={processando}
          >
            <option value="">Selecione um evento…</option>
            {opcoesParaInscrever.map((e) => (
              <option key={e.id} value={e.id}>{e.titulo} — {formatarData(e.dataHora)}</option>
            ))}
          </select>
        </label>
        <button className={styles.actionButton} onClick={inscrever} disabled={!eventoSelecionado || processando}>
          {processando ? 'Inscrevendo…' : 'Inscrever'}
        </button>
      </div>
      {error && <div className={styles.error}>{error}</div>}

      {!error && inscricoes === null && <div className={styles.loading}>Carregando…</div>}
      {inscricoes?.length === 0 && <div className={styles.muted}>Nenhuma inscrição em evento ainda.</div>}
      {inscricoes && inscricoes.length > 0 && (
        <div className={styles.mentoriasList} style={{ marginTop: 12 }}>
          {inscricoes.map((i) => {
            const st = STATUS_INSCRICAO_LABEL[i.status];
            return (
              <div key={i.eventoId} className={styles.mentoriaRow}>
                <div className={styles.mentoriaData}>{formatarData(i.dataHora)}</div>
                <div className={styles.mentoriaMentor}>{i.titulo}</div>
                <Pill bg={st.bg} color={st.color}>{st.label}</Pill>
                {i.status === 'INSCRITA' && (
                  <button className={styles.actionButtonDanger} onClick={() => cancelar(i.eventoId)} disabled={processando}>
                    Cancelar inscrição
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </Card>
  );
}
