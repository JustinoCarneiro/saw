import { type FormEvent, useEffect, useState } from 'react';
import { isAxiosError } from 'axios';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { useAuth } from '../auth/AuthContext';
import { Card } from '../../shared/components/Card';
import { ConfirmDialog } from '../../shared/components/ConfirmDialog';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { formatarTelefone } from '../../shared/lib/format';
import type {
  ImportMentoradoDiretoResultResponse,
  Lead,
  MentoradoAdmin,
  MentoradoCriado,
  StatusMentorado,
  TipoContrato,
} from '../../shared/lib/types';
import styles from './MentoradosListaPage.module.css';

const COLUMNS = '1.4fr 1.6fr 1.2fr 1fr 1fr 1.6fr';

// M23 criou /direto e /dados-contrato atrás de Modulo.COMERCIAL (achado do revisor-seguranca:
// CNPJ/sócios/valor de contrato são dado comercial sensível). Pedido do Marcos (22/07/2026)
// reverteu isso: Gestão de Performance também precisa de acesso pleno aqui — o backend aceita
// COMERCIAL OU MENTORADOS agora (ver RequiresModulo em MentoradoContratoController).
const TIPO_CONTRATO_LABEL: Record<TipoContrato, string> = {
  MENTORIA_CONTINUA: 'Mentoria Contínua',
  MENTORIA_INDIVIDUAL: 'Mentoria Individual',
  CONSULTORIA: 'Consultoria',
};

const STATUS_LABEL: Record<StatusMentorado, { label: string; bg: string; color: string }> = {
  ATIVO: { label: 'Ativo', bg: 'var(--success-bg)', color: 'var(--success)' },
  INATIVO: { label: 'Inativo', bg: 'var(--line)', color: 'var(--text-soft)' },
};

export function MentoradosListaPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  // 22/07/2026 — quem enxerga esta tela (Modulo.MENTORADOS) já pode ver/criar contrato; COMERCIAL
  // continua valendo também, pra não tirar acesso de quem já tinha.
  const podeVerContrato =
    (user?.modulosPermitidos.includes('COMERCIAL') || user?.modulosPermitidos.includes('MENTORADOS')) ?? false;

  const [status, setStatus] = useState<StatusMentorado | ''>('');
  const [busca, setBusca] = useState('');
  const [mentorados, setMentorados] = useState<MentoradoAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [criandoDireto, setCriandoDireto] = useState(false);
  const [criado, setCriado] = useState<MentoradoCriado | null>(null);
  const [importandoDireto, setImportandoDireto] = useState(false);
  const [resultadoImportDireto, setResultadoImportDireto] = useState<ImportMentoradoDiretoResultResponse | null>(null);

  const carregar = () => {
    setMentorados(null);
    apiClient
      .get<MentoradoAdmin[]>('/admin/mentorados', { params: { status: status || undefined, busca: busca || undefined } })
      .then((res) => setMentorados(res.data))
      .catch(() => setError('Não foi possível carregar os mentorados.'));
  };

  useEffect(carregar, [status, busca]);

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.filters}>
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
              <span style={{ fontSize: 16 }}>+</span>Importar mentorados (CSV)
            </button>
          )}
          <button className={styles.newButton} onClick={() => setCriando(true)}>
            <span style={{ fontSize: 16 }}>+</span>Criar a partir de um lead
          </button>
        </div>
      </div>

      <div className={styles.csvRow}>
        {/* M28 ("import único", 21/07/2026) — o import estreito (bulk-UPDATE de 6 campos) que
            ficava aqui foi removido: dois botões de import confundiam o time (ver ROADMAP.md § M28).
            Import agora é só o widget "Importar mentorados (CSV)" acima (Comercial), que cria OU
            atualiza; esta tela mantém só a exportação. */}
        <CsvImportExport
          exportUrl="/admin/mentorados/export"
          exportParams={{ status: status || undefined, busca: busca || undefined }}
          exportFilename="mentorados.csv"
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
          <div className={styles.formTitle}>
            {resultadoImportDireto.importados} criado(s), {resultadoImportDireto.atualizados} atualizado(s)
          </div>
          {resultadoImportDireto.criados.length > 0 && (
            <>
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
            </>
          )}
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

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Nome', 'Negócio', 'E-mail', 'Tipo de contrato', 'Status', 'Ações']}>
        {mentorados === null && !error && <div className={styles.loading}>Carregando…</div>}
        {mentorados?.length === 0 && <div className={styles.loading}>Nenhum mentorado encontrado.</div>}
        {mentorados?.map((m) => {
          const st = STATUS_LABEL[m.status];
          return (
            <DataGridRow key={m.id} columns={COLUMNS} testId={`mentorado-row-${m.id}`}>
              <div className={styles.strong}>{m.nome}</div>
              <div className={styles.muted}>{m.negocio ?? '—'}</div>
              <div className={`${styles.muted} ${styles.email}`}>{m.email}</div>
              <div className={styles.muted}>{m.tipoContrato ? TIPO_CONTRATO_LABEL[m.tipoContrato] : '—'}</div>
              <div><Pill bg={st.bg} color={st.color}>{st.label}</Pill></div>
              <div className={styles.acoes}>
                {/* M28 ("página dedicada de mentorado") — antes expandia um form inline nesta
                    mesma tela; agora navega pra uma página própria (foco total, estilo Notion),
                    fora das abas da MentoradosShell. Ver App.tsx e MentoradoDetalhePage.tsx. */}
                <button className={styles.actionButton} onClick={() => navigate(`/admin/mentorados/lista/${m.id}`)}>
                  Ver perfil
                </button>
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
            <input className={styles.textInput} value={telefone} onChange={(e) => setTelefone(formatarTelefone(e.target.value))}
                   placeholder="(11) 90000-0000" maxLength={15} />
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

// M23 item 4 (bulk-CREATE, 19/07/2026), estendido no M28 ("import único", 21/07/2026) — import CSV
// que CRIA mentorados novos OU ATUALIZA quem já existe (resolvido por e-mail), pensado pra migrar
// de uma vez as empresas reais que hoje só existem no Notion. Só COMERCIAL/ADMIN veem o botão que
// abre este form (mesmo gate de "Criar mentorado direto" — cria credencial + carrega CNPJ/sócios/
// valor de contrato).
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
      <div className={styles.formTitle}>Importar mentorados (CSV)</div>
      <p className={styles.muted} style={{ marginTop: -8, marginBottom: 4 }}>
        Por linha do CSV: se o e-mail ainda não existe, cria um mentorado novo (com Lead já fechado
        + credencial de login); se já existe um mentorado com esse e-mail, atualiza os dados dele em
        vez de criar de novo. Tudo-ou-nada: se alguma linha tiver erro, nada é criado ou atualizado.
        Colunas esperadas (só email/nome/tipoContrato são obrigatórias):{' '}
        <code>{COLUNAS_IMPORT_DIRETO.join(', ')}</code>.
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
