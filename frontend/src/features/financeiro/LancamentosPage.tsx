import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import type {
  CategoriaFinanceira,
  EventoResumoFinanceiro,
  FormaPagamentoLancamento,
  GrupoDre,
  Lancamento,
  NaturezaFinanceira,
  OrigemReceita,
  StatusLancamento,
  TipoLancamento,
} from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import styles from './LancamentosPage.module.css';

// Change request do cliente (20/07/2026) — "Lançamentos" e "Contas a pagar/receber" eram 2 abas
// separadas mostrando a MESMA tabela (unificada desde o M26) por 2 lentes diferentes; o cliente
// achou redundante ter 2 telas pra um dado só. Fundidas numa tela única. Base de listagem
// continua sendo GET /admin/financeiro/lancamentos (filtro por dataCompetencia, sempre
// preenchida) — NÃO GET /admin/financeiro/contas (filtro por dataVencimento, que por construção
// nunca bate NULL — um lançamento criado direto como Realizado, sem vencimento, sumiria da tela
// se a base fosse esse endpoint). Liquidar/Parcial usam os endpoints de .../lancamentos/{id}/...,
// idênticos aos de .../contas/{id}/... (mesmo LancamentoService por baixo).
const COLUMNS = '.9fr 1.6fr 1fr .9fr .9fr 1.1fr .9fr 1.3fr';

// Pedido do Marcos (22/07/2026) — mesma riqueza da coluna "Forma de Pagamento" da planilha real.
const FORMA_PAGAMENTO_LABEL: Record<FormaPagamentoLancamento, string> = {
  PIX: 'Pix',
  PIX_RECORRENTE: 'Pix Recorrente',
  CARTAO: 'Cartão',
  BOLETO: 'Boleto',
  HOTMART: 'Hotmart',
};

function primeiroDiaDoMes(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

function ultimoDiaDoMes(): string {
  const d = new Date();
  const ultimo = new Date(d.getFullYear(), d.getMonth() + 1, 0);
  return ultimo.toISOString().slice(0, 10);
}

// GET /admin/financeiro/lancamentos exige de/ate (sempre exigiu, ver LancamentoController) — pra
// "filtro de período desligado" (default desta tela fundida, mesmo comportamento que "Contas a
// pagar/receber" já tinha) manda uma janela bem larga em vez de omitir o parâmetro. Uma parcela
// futura (ex.: vencimento daqui a 2 meses, dataCompetencia = mesma data) precisa aparecer por
// padrão — só some se o usuário ligar o filtro de período de propósito.
const SEM_FILTRO_DE = '1900-01-01';
const SEM_FILTRO_ATE = '2999-12-31';

function statusLabel(l: Lancamento): { label: string; bg: string; color: string } {
  if (l.status === 'REALIZADO') {
    return l.tipo === 'DESPESA'
      ? { label: 'Pago', bg: 'var(--success-bg)', color: 'var(--success)' }
      : { label: 'Recebido', bg: 'var(--success-bg)', color: 'var(--success)' };
  }
  if (l.status === 'PARCIAL') return { label: 'Parcial', bg: 'var(--warning-bg)', color: 'var(--warning)' };
  if (l.status === 'VENCIDO') return { label: 'Vencido', bg: 'var(--danger-bg)', color: 'var(--danger)' };
  return { label: 'Previsto', bg: 'var(--info-bg)', color: 'var(--info)' };
}

const GRUPO_DRE_LABEL: Record<GrupoDre, string> = {
  RECEITA_BRUTA: 'Receita Bruta',
  DEDUCOES: 'Deduções',
  CUSTOS: 'Custos',
  DESPESA_OPERACIONAL: 'Despesa Operacional',
};

const ORIGEM_RECEITA_LABEL: Record<OrigemReceita, string> = {
  ASSINATURA: 'Assinatura',
  LOJA: 'Loja',
  EVENTO: 'Evento',
  OUTRA: 'Outra',
};

export function LancamentosPage() {
  const [filtroPeriodoLigado, setFiltroPeriodoLigado] = useState(false);
  const [de, setDe] = useState(primeiroDiaDoMes());
  const [ate, setAte] = useState(ultimoDiaDoMes());
  const [tipo, setTipo] = useState<TipoLancamento | ''>('');
  const [status, setStatus] = useState<StatusLancamento | ''>('');
  const [eventoId, setEventoId] = useState('');
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamentoLancamento | ''>('');
  const [lancamentos, setLancamentos] = useState<Lancamento[] | null>(null);
  const [categorias, setCategorias] = useState<CategoriaFinanceira[]>([]);
  const [eventos, setEventos] = useState<EventoResumoFinanceiro[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [showCategoriaForm, setShowCategoriaForm] = useState(false);
  const [liquidando, setLiquidando] = useState<Lancamento | null>(null);
  const [liquidandoParcial, setLiquidandoParcial] = useState<Lancamento | null>(null);

  const carregar = () => {
    setLancamentos(null);
    apiClient
      .get<Lancamento[]>('/admin/financeiro/lancamentos', {
        params: {
          de: filtroPeriodoLigado ? de : SEM_FILTRO_DE,
          ate: filtroPeriodoLigado ? ate : SEM_FILTRO_ATE,
          tipo: tipo || undefined, status: status || undefined, eventoId: eventoId || undefined,
          formaPagamento: formaPagamento || undefined,
        },
      })
      .then((res) => setLancamentos(res.data))
      .catch(() => setError('Não foi possível carregar os lançamentos.'));
  };

  const carregarCategorias = () => {
    apiClient.get<CategoriaFinanceira[]>('/admin/financeiro/categorias')
      .then((res) => setCategorias(res.data))
      .catch(() => setError('Não foi possível carregar as categorias financeiras.'));
  };

  useEffect(carregar, [filtroPeriodoLigado, de, ate, tipo, status, eventoId, formaPagamento]);

  useEffect(() => {
    carregarCategorias();
    apiClient.get<EventoResumoFinanceiro[]>('/admin/financeiro/eventos')
      .then((res) => setEventos(res.data))
      .catch(() => setError('Não foi possível carregar a lista de eventos.'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <label className={styles.checkboxField}>
            <input type="checkbox" checked={filtroPeriodoLigado} onChange={(e) => setFiltroPeriodoLigado(e.target.checked)} />
            Filtrar por período
          </label>
          {filtroPeriodoLigado && (
            <>
              <label className={styles.filterLabel}>
                De
                <input type="date" className={styles.dateInput} value={de} onChange={(e) => setDe(e.target.value)} />
              </label>
              <label className={styles.filterLabel}>
                Até
                <input type="date" className={styles.dateInput} value={ate} onChange={(e) => setAte(e.target.value)} />
              </label>
            </>
          )}
          <select className={styles.select} value={tipo} onChange={(e) => setTipo(e.target.value as TipoLancamento | '')}>
            <option value="">Todos os tipos</option>
            <option value="RECEITA">Receita</option>
            <option value="DESPESA">Despesa</option>
          </select>
          <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusLancamento | '')}>
            <option value="">Todos os status</option>
            <option value="PREVISTO">Previsto</option>
            <option value="PARCIAL">Parcial</option>
            <option value="REALIZADO">Pago/Recebido</option>
            <option value="VENCIDO">Vencido</option>
          </select>
          <select className={styles.select} value={eventoId} onChange={(e) => setEventoId(e.target.value)}>
            <option value="">Todos os eventos</option>
            {eventos.map((ev) => (
              <option key={ev.id} value={ev.id}>{ev.titulo}</option>
            ))}
          </select>
          <select className={styles.select} aria-label="Filtrar por forma de pagamento" value={formaPagamento}
                  onChange={(e) => setFormaPagamento(e.target.value as FormaPagamentoLancamento | '')}>
            <option value="">Todas as formas de pagamento</option>
            {Object.entries(FORMA_PAGAMENTO_LABEL).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
          <CsvImportExport
            exportUrl="/admin/financeiro/lancamentos/export"
            exportParams={{
              de: filtroPeriodoLigado ? de : SEM_FILTRO_DE,
              ate: filtroPeriodoLigado ? ate : SEM_FILTRO_ATE,
              tipo: tipo || undefined,
            }}
            exportFilename="lancamentos.csv"
            importUrl="/admin/financeiro/lancamentos/import"
            onImportado={carregar}
          />
          <button className={styles.outlineButton} onClick={() => setShowCategoriaForm((v) => !v)}>
            <span style={{ fontSize: 16 }}>+</span>Nova categoria
          </button>
          <button className={styles.newButton} onClick={() => setShowForm((v) => !v)}>
            <span style={{ fontSize: 16 }}>+</span>Novo lançamento
          </button>
        </div>
      </div>

      {showCategoriaForm && (
        <NovaCategoriaForm
          onCriado={() => {
            setShowCategoriaForm(false);
            carregarCategorias();
          }}
          onCancelar={() => setShowCategoriaForm(false)}
        />
      )}

      {showForm && (
        <NovoLancamentoForm
          categorias={categorias}
          eventos={eventos}
          onCriado={() => {
            setShowForm(false);
            carregar();
          }}
          onCancelar={() => setShowForm(false)}
        />
      )}

      {liquidando && (
        <LiquidarLancamentoForm
          lancamento={liquidando}
          onLiquidado={() => { setLiquidando(null); carregar(); }}
          onCancelar={() => setLiquidando(null)}
        />
      )}

      {liquidandoParcial && (
        <LiquidarParcialLancamentoForm
          lancamento={liquidandoParcial}
          onLiquidado={() => { setLiquidandoParcial(null); carregar(); }}
          onCancelar={() => setLiquidandoParcial(null)}
        />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Data', 'Descrição', 'Subcategoria', 'Pagamento', 'Vencimento', 'Valor', 'Status', 'Ações']}>
        {lancamentos === null && !error && <div className={styles.loading}>Carregando…</div>}
        {lancamentos?.length === 0 && <div className={styles.loading}>Nenhum lançamento neste período.</div>}
        {lancamentos?.map((l) => {
          const st = statusLabel(l);
          const podeLiquidar = l.status === 'PREVISTO' || l.status === 'VENCIDO' || l.status === 'PARCIAL';
          return (
            <DataGridRow key={l.id} columns={COLUMNS} testId="lancamento-row">
              <div className={styles.muted}>{new Date(l.dataCompetencia + 'T00:00:00').toLocaleDateString('pt-BR')}</div>
              <div>
                <div className={styles.strong}>{l.descricao}</div>
                {l.eventoTitulo && <div className={styles.muted}>{l.eventoTitulo}</div>}
              </div>
              <div className={styles.muted}>{l.categoria.nome}</div>
              <div className={styles.muted}>{l.formaPagamento ? FORMA_PAGAMENTO_LABEL[l.formaPagamento] : '—'}</div>
              <div className={styles.muted}>
                {l.dataVencimento ? new Date(l.dataVencimento + 'T00:00:00').toLocaleDateString('pt-BR') : '—'}
              </div>
              <div className={styles.valor} style={{ color: l.tipo === 'RECEITA' ? 'var(--success)' : 'var(--danger)' }}>
                {l.tipo === 'RECEITA' ? '+' : '−'}
                {formatBRL(l.valor)}
                {l.status === 'PARCIAL' && l.valorPago != null && (
                  <div className={styles.muted}>pago: {formatBRL(l.valorPago)}</div>
                )}
              </div>
              <div>
                <Pill bg={st.bg} color={st.color}>{st.label}</Pill>
              </div>
              <div>
                {podeLiquidar ? (
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button className={styles.liquidarButton} onClick={() => setLiquidando(l)}>
                      Liquidar
                    </button>
                    <button className={styles.liquidarButton} onClick={() => setLiquidandoParcial(l)}>
                      Parcial
                    </button>
                  </div>
                ) : (
                  <span className={styles.muted}>—</span>
                )}
              </div>
            </DataGridRow>
          );
        })}
      </DataGrid>
    </div>
  );
}

function NovaCategoriaForm({ onCriado, onCancelar }: {
  onCriado: () => void;
  onCancelar: () => void;
}) {
  const [nome, setNome] = useState('');
  const [tipo, setTipo] = useState<TipoLancamento>('RECEITA');
  const [grupoDre, setGrupoDre] = useState<GrupoDre>('RECEITA_BRUTA');
  const [origemReceita, setOrigemReceita] = useState<OrigemReceita | ''>('');
  // E14 — subcategorias fixo/variável (raio-x da planilha real "DRE Financeira Saw"): grupo é
  // texto livre (departamento/linha, ex. "Estrutura"), natureza é opcional pros dois lados.
  const [grupo, setGrupo] = useState('');
  const [natureza, setNatureza] = useState<NaturezaFinanceira | ''>('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/financeiro/categorias', {
        nome, tipo, grupoDre, origemReceita: tipo === 'RECEITA' && origemReceita ? origemReceita : null,
        grupo: grupo || null, natureza: natureza || null,
      });
      onCriado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar a categoria. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Subcategoria
            <input className={styles.textInput} placeholder="Ex.: Aluguel, Água Mineral…" value={nome} onChange={(e) => setNome(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Tipo
            <select className={styles.select} value={tipo} onChange={(e) => { setTipo(e.target.value as TipoLancamento); setOrigemReceita(''); }}>
              <option value="RECEITA">Receita</option>
              <option value="DESPESA">Despesa</option>
            </select>
          </label>
          <label className={styles.formField}>
            Grupo DRE
            <select className={styles.select} value={grupoDre} onChange={(e) => setGrupoDre(e.target.value as GrupoDre)}>
              {Object.entries(GRUPO_DRE_LABEL).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          {tipo === 'RECEITA' && (
            <label className={styles.formField}>
              Origem da receita
              <select className={styles.select} value={origemReceita} onChange={(e) => setOrigemReceita(e.target.value as OrigemReceita | '')}>
                <option value="">Nenhuma</option>
                {Object.entries(ORIGEM_RECEITA_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>
          )}
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Categoria (opcional)
            <input className={styles.textInput} placeholder="Ex.: Estrutura, Pessoas…" value={grupo} onChange={(e) => setGrupo(e.target.value)} />
          </label>
          <label className={styles.formField}>
            Natureza (opcional)
            <select className={styles.select} value={natureza} onChange={(e) => setNatureza(e.target.value as NaturezaFinanceira | '')}>
              <option value="">Nenhuma</option>
              <option value="FIXA">Fixa</option>
              <option value="VARIAVEL">Variável</option>
            </select>
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar categoria'}
          </button>
        </div>
      </form>
    </Card>
  );
}

// Fusão de "Novo lançamento" + "Nova conta" (change request 20/07/2026). "Já foi realizado?"
// decide o resto do form: Sim -> pede Data de competência, nasce REALIZADO, sem vencimento
// (fluxo antigo de "Novo lançamento"); Não -> pede Vencimento, nasce PREVISTO com
// dataCompetencia = dataVencimento (melhor palpite disponível até liquidar — mesmo
// comportamento que "Nova conta" já tinha, ver LancamentoFinanceiro no backend).
function NovoLancamentoForm({ categorias, eventos, onCriado, onCancelar }: {
  categorias: CategoriaFinanceira[];
  eventos: EventoResumoFinanceiro[];
  onCriado: () => void;
  onCancelar: () => void;
}) {
  const [tipo, setTipo] = useState<TipoLancamento>('RECEITA');
  const [categoriaId, setCategoriaId] = useState('');
  const [descricao, setDescricao] = useState('');
  const [valor, setValor] = useState('');
  const [jaRealizado, setJaRealizado] = useState(true);
  const [data, setData] = useState(new Date().toISOString().slice(0, 10));
  const [eventoId, setEventoId] = useState('');
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamentoLancamento | ''>('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const categoriasDoTipo = categorias.filter((c) => c.tipo === tipo);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!categoriaId) {
      setError('Selecione uma categoria.');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post('/admin/financeiro/lancamentos', {
        tipo, categoriaId, descricao, valor: Number(valor), eventoId: eventoId || null,
        status: jaRealizado ? 'REALIZADO' : 'PREVISTO',
        dataCompetencia: data,
        dataVencimento: jaRealizado ? null : data,
        formaPagamento: formaPagamento || null,
      });
      onCriado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar o lançamento. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Tipo
            <select className={styles.select} value={tipo} onChange={(e) => { setTipo(e.target.value as TipoLancamento); setCategoriaId(''); }}>
              <option value="RECEITA">Receita</option>
              <option value="DESPESA">Despesa</option>
            </select>
          </label>
          <label className={styles.formField}>
            Subcategoria
            <select className={styles.select} value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)} required>
              <option value="">{categoriasDoTipo.length === 0 ? 'Nenhuma subcategoria cadastrada' : 'Selecione…'}</option>
              {categoriasDoTipo.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
            {categoriasDoTipo.length === 0 && (
              <div className={styles.muted}>
                Nenhuma subcategoria de {tipo === 'RECEITA' ? 'receita' : 'despesa'} cadastrada — crie uma em "+ Nova categoria".
              </div>
            )}
          </label>
          <label className={styles.formField}>
            Já foi realizado?
            <select className={styles.select} value={jaRealizado ? 'sim' : 'nao'} onChange={(e) => setJaRealizado(e.target.value === 'sim')}>
              <option value="sim">Sim (já aconteceu)</option>
              <option value="nao">Não (previsto)</option>
            </select>
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Descrição
            <input className={styles.textInput} value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Valor (R$)
            <input className={styles.textInput} type="number" min="0.01" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            {jaRealizado ? 'Data de competência' : 'Vencimento'}
            <input className={styles.textInput} type="date" value={data} onChange={(e) => setData(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Evento (opcional)
            <select className={styles.select} value={eventoId} onChange={(e) => setEventoId(e.target.value)}>
              <option value="">Sem evento</option>
              {eventos.map((ev) => (
                <option key={ev.id} value={ev.id}>{ev.titulo}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Forma de pagamento (opcional)
            <select className={styles.select} value={formaPagamento}
                    onChange={(e) => setFormaPagamento(e.target.value as FormaPagamentoLancamento | '')}>
              <option value="">Não informada</option>
              {Object.entries(FORMA_PAGAMENTO_LABEL).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar lançamento'}
          </button>
        </div>
      </form>
    </Card>
  );
}

function LiquidarLancamentoForm({ lancamento, onLiquidado, onCancelar }: {
  lancamento: Lancamento;
  onLiquidado: () => void;
  onCancelar: () => void;
}) {
  const [dataPagamento, setDataPagamento] = useState(new Date().toISOString().slice(0, 10));
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/financeiro/lancamentos/${lancamento.id}/liquidar`, { dataPagamento });
      onLiquidado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível liquidar. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.liquidarTitle}>
        Liquidar: {lancamento.descricao} — {formatBRL(lancamento.valor)}
      </div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Data do {lancamento.tipo === 'DESPESA' ? 'pagamento' : 'recebimento'}
            <input className={styles.textInput} type="date" value={dataPagamento} onChange={(e) => setDataPagamento(e.target.value)} required />
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Liquidando…' : 'Confirmar liquidação'}
          </button>
        </div>
      </form>
    </Card>
  );
}

// Gap 1 (raio-x, 18/07/2026) — pagamento parcial, acumulativo (ver
// LancamentoFinanceiro#liquidarParcial no backend). Não oferece "Gerar lançamento": diferente da
// liquidação total, o valor de um pagamento parcial não corresponde ao valor cheio do lançamento.
function LiquidarParcialLancamentoForm({ lancamento, onLiquidado, onCancelar }: {
  lancamento: Lancamento;
  onLiquidado: () => void;
  onCancelar: () => void;
}) {
  const valorRestante = lancamento.valor - (lancamento.valorPago ?? 0);
  const [valorPago, setValorPago] = useState('');
  const [dataPagamento, setDataPagamento] = useState(new Date().toISOString().slice(0, 10));
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/financeiro/lancamentos/${lancamento.id}/liquidar-parcial`, {
        valorPago: Number(valorPago), dataPagamento,
      });
      onLiquidado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível registrar o pagamento parcial. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.liquidarTitle}>
        Pagamento parcial: {lancamento.descricao} — falta {formatBRL(valorRestante)} de {formatBRL(lancamento.valor)}
      </div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Valor pago agora (R$)
            <input className={styles.textInput} type="number" min="0.01" max={valorRestante} step="0.01"
                   value={valorPago} onChange={(e) => setValorPago(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Data do {lancamento.tipo === 'DESPESA' ? 'pagamento' : 'recebimento'}
            <input className={styles.textInput} type="date" value={dataPagamento} onChange={(e) => setDataPagamento(e.target.value)} required />
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Registrar pagamento parcial'}
          </button>
        </div>
      </form>
    </Card>
  );
}
