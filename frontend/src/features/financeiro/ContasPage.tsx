import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import type { CategoriaFinanceira, Conta, StatusConta, TipoConta } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import styles from './ContasPage.module.css';

const COLUMNS = '1.2fr 2fr 1fr 1fr 1fr 1fr';

const TIPO_LABEL: Record<TipoConta, string> = { A_PAGAR: 'A pagar', A_RECEBER: 'A receber' };

const STATUS_LABEL: Record<StatusConta, { label: string; bg: string; color: string }> = {
  PENDENTE: { label: 'Pendente', bg: 'var(--warning-bg)', color: 'var(--warning)' },
  PAGO: { label: 'Pago', bg: 'var(--success-bg)', color: 'var(--success)' },
  RECEBIDO: { label: 'Recebido', bg: 'var(--success-bg)', color: 'var(--success)' },
  VENCIDO: { label: 'Vencido', bg: 'var(--danger-bg)', color: 'var(--danger)' },
};

export function ContasPage() {
  const [tipo, setTipo] = useState<TipoConta | ''>('');
  const [status, setStatus] = useState<StatusConta | ''>('');
  const [contas, setContas] = useState<Conta[] | null>(null);
  const [categorias, setCategorias] = useState<CategoriaFinanceira[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [liquidando, setLiquidando] = useState<Conta | null>(null);

  const carregar = () => {
    setContas(null);
    apiClient
      .get<Conta[]>('/admin/financeiro/contas', { params: { tipo: tipo || undefined, status: status || undefined } })
      .then((res) => setContas(res.data))
      .catch(() => setError('Não foi possível carregar as contas.'));
  };

  useEffect(carregar, [tipo, status]);

  useEffect(() => {
    apiClient.get<CategoriaFinanceira[]>('/admin/financeiro/categorias')
      .then((res) => setCategorias(res.data))
      .catch(() => setError('Não foi possível carregar as categorias financeiras.'));
  }, []);

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <select className={styles.select} value={tipo} onChange={(e) => setTipo(e.target.value as TipoConta | '')}>
            <option value="">Todos os tipos</option>
            <option value="A_PAGAR">A pagar</option>
            <option value="A_RECEBER">A receber</option>
          </select>
          <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusConta | '')}>
            <option value="">Todos os status</option>
            <option value="PENDENTE">Pendente</option>
            <option value="PAGO">Pago</option>
            <option value="RECEBIDO">Recebido</option>
            <option value="VENCIDO">Vencido</option>
          </select>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
          <CsvImportExport
            exportUrl="/admin/financeiro/contas/export"
            exportParams={{ tipo: tipo || undefined, status: status || undefined }}
            exportFilename="contas.csv"
            importUrl="/admin/financeiro/contas/import"
            onImportado={carregar}
          />
          <button className={styles.newButton} onClick={() => setShowForm((v) => !v)}>
            <span style={{ fontSize: 16 }}>+</span>Nova conta
          </button>
        </div>
      </div>

      {showForm && (
        <NovaContaForm
          categorias={categorias}
          onCriado={() => { setShowForm(false); carregar(); }}
          onCancelar={() => setShowForm(false)}
        />
      )}

      {liquidando && (
        <LiquidarContaForm
          conta={liquidando}
          onLiquidado={() => { setLiquidando(null); carregar(); }}
          onCancelar={() => setLiquidando(null)}
        />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Tipo', 'Descrição', 'Valor', 'Vencimento', 'Status', 'Ações']}>
        {contas === null && !error && <div className={styles.loading}>Carregando…</div>}
        {contas?.length === 0 && <div className={styles.loading}>Nenhuma conta encontrada.</div>}
        {contas?.map((c) => {
          const st = STATUS_LABEL[c.status];
          const podeLiquidar = c.status === 'PENDENTE' || c.status === 'VENCIDO';
          return (
            <DataGridRow key={c.id} columns={COLUMNS}>
              <div className={styles.muted}>{TIPO_LABEL[c.tipo]}</div>
              <div className={styles.strong}>{c.descricao}</div>
              <div className={styles.valor}>{formatBRL(c.valor)}</div>
              <div className={styles.muted}>{new Date(c.dataVencimento + 'T00:00:00').toLocaleDateString('pt-BR')}</div>
              <div>
                <Pill bg={st.bg} color={st.color}>{st.label}</Pill>
              </div>
              <div>
                {podeLiquidar ? (
                  <button className={styles.liquidarButton} onClick={() => setLiquidando(c)}>
                    Liquidar
                  </button>
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

function NovaContaForm({ categorias, onCriado, onCancelar }: {
  categorias: CategoriaFinanceira[];
  onCriado: () => void;
  onCancelar: () => void;
}) {
  const [tipo, setTipo] = useState<TipoConta>('A_PAGAR');
  const [descricao, setDescricao] = useState('');
  const [valor, setValor] = useState('');
  const [dataVencimento, setDataVencimento] = useState(new Date().toISOString().slice(0, 10));
  const [categoriaId, setCategoriaId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // A_PAGAR nasce de uma despesa (custo/despesa operacional); A_RECEBER, de uma receita.
  const categoriasDoTipo = categorias.filter((c) => (tipo === 'A_PAGAR' ? c.tipo === 'DESPESA' : c.tipo === 'RECEITA'));

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/financeiro/contas', {
        tipo, descricao, valor: Number(valor), dataVencimento, categoriaId: categoriaId || null,
      });
      onCriado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar a conta. Confira os dados.'));
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
            <select className={styles.select} value={tipo} onChange={(e) => { setTipo(e.target.value as TipoConta); setCategoriaId(''); }}>
              <option value="A_PAGAR">A pagar</option>
              <option value="A_RECEBER">A receber</option>
            </select>
          </label>
          <label className={styles.formField} style={{ flex: 2 }}>
            Descrição
            <input className={styles.textInput} value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Valor (R$)
            <input className={styles.textInput} type="number" min="0.01" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Vencimento
            <input className={styles.textInput} type="date" value={dataVencimento} onChange={(e) => setDataVencimento(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Categoria (opcional)
            <select className={styles.select} value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}>
              <option value="">Sem categoria</option>
              {categoriasDoTipo.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar conta'}
          </button>
        </div>
      </form>
    </Card>
  );
}

function LiquidarContaForm({ conta, onLiquidado, onCancelar }: {
  conta: Conta;
  onLiquidado: () => void;
  onCancelar: () => void;
}) {
  const [dataPagamento, setDataPagamento] = useState(new Date().toISOString().slice(0, 10));
  const [criarLancamento, setCriarLancamento] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.patch(`/admin/financeiro/contas/${conta.id}/liquidar`, { dataPagamento, criarLancamento });
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
        Liquidar: {conta.descricao} — {formatBRL(conta.valor)}
      </div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Data do {conta.tipo === 'A_PAGAR' ? 'pagamento' : 'recebimento'}
            <input className={styles.textInput} type="date" value={dataPagamento} onChange={(e) => setDataPagamento(e.target.value)} required />
          </label>
          <label className={styles.checkboxField}>
            <input type="checkbox" checked={criarLancamento} onChange={(e) => setCriarLancamento(e.target.checked)} />
            Gerar lançamento financeiro correspondente
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
