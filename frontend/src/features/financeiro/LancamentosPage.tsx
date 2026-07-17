import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import type { CategoriaFinanceira, GrupoDre, Lancamento, OrigemReceita, StatusLancamento, TipoLancamento } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import styles from './LancamentosPage.module.css';

const COLUMNS = '1fr 2fr 1.5fr 1fr 1fr .6fr';

function primeiroDiaDoMes(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

function ultimoDiaDoMes(): string {
  const d = new Date();
  const ultimo = new Date(d.getFullYear(), d.getMonth() + 1, 0);
  return ultimo.toISOString().slice(0, 10);
}

const STATUS_LABEL: Record<StatusLancamento, { label: string; bg: string; color: string }> = {
  PREVISTO: { label: 'Previsto', bg: 'var(--info-bg)', color: 'var(--info)' },
  REALIZADO: { label: 'Realizado', bg: 'var(--success-bg)', color: 'var(--success)' },
};

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
  const [de, setDe] = useState(primeiroDiaDoMes());
  const [ate, setAte] = useState(ultimoDiaDoMes());
  const [tipo, setTipo] = useState<TipoLancamento | ''>('');
  const [lancamentos, setLancamentos] = useState<Lancamento[] | null>(null);
  const [categorias, setCategorias] = useState<CategoriaFinanceira[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [showCategoriaForm, setShowCategoriaForm] = useState(false);

  const carregar = () => {
    setLancamentos(null);
    apiClient
      .get<Lancamento[]>('/admin/financeiro/lancamentos', { params: { de, ate, tipo: tipo || undefined } })
      .then((res) => setLancamentos(res.data))
      .catch(() => setError('Não foi possível carregar os lançamentos.'));
  };

  const carregarCategorias = () => {
    apiClient.get<CategoriaFinanceira[]>('/admin/financeiro/categorias')
      .then((res) => setCategorias(res.data))
      .catch(() => setError('Não foi possível carregar as categorias financeiras.'));
  };

  useEffect(carregar, [de, ate, tipo]);

  useEffect(carregarCategorias, []);

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.filters}>
          <label className={styles.filterLabel}>
            De
            <input type="date" className={styles.dateInput} value={de} onChange={(e) => setDe(e.target.value)} />
          </label>
          <label className={styles.filterLabel}>
            Até
            <input type="date" className={styles.dateInput} value={ate} onChange={(e) => setAte(e.target.value)} />
          </label>
          <select className={styles.select} value={tipo} onChange={(e) => setTipo(e.target.value as TipoLancamento | '')}>
            <option value="">Todos os tipos</option>
            <option value="RECEITA">Receita</option>
            <option value="DESPESA">Despesa</option>
          </select>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
          <CsvImportExport
            exportUrl="/admin/financeiro/lancamentos/export"
            exportParams={{ de, ate, tipo: tipo || undefined }}
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
          onCriado={() => {
            setShowForm(false);
            carregar();
          }}
          onCancelar={() => setShowForm(false)}
        />
      )}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Data', 'Descrição', 'Categoria', 'Valor', 'Status', 'Plano']}>
        {lancamentos === null && !error && <div className={styles.loading}>Carregando…</div>}
        {lancamentos?.length === 0 && <div className={styles.loading}>Nenhum lançamento neste período.</div>}
        {lancamentos?.map((l) => {
          const status = STATUS_LABEL[l.status];
          return (
            <DataGridRow key={l.id} columns={COLUMNS}>
              <div className={styles.muted}>{new Date(l.dataCompetencia + 'T00:00:00').toLocaleDateString('pt-BR')}</div>
              <div className={styles.strong}>{l.descricao}</div>
              <div className={styles.muted}>{l.categoria.nome}</div>
              <div className={styles.valor} style={{ color: l.tipo === 'RECEITA' ? 'var(--success)' : 'var(--danger)' }}>
                {l.tipo === 'RECEITA' ? '+' : '−'}
                {formatBRL(l.valor)}
              </div>
              <div>
                <Pill bg={status.bg} color={status.color}>
                  {status.label}
                </Pill>
              </div>
              <div className={styles.muted}>{l.planoReferencia ?? '—'}</div>
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
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/financeiro/categorias', {
        nome, tipo, grupoDre, origemReceita: tipo === 'RECEITA' && origemReceita ? origemReceita : null,
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
            Nome
            <input className={styles.textInput} value={nome} onChange={(e) => setNome(e.target.value)} required />
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

function NovoLancamentoForm({ categorias, onCriado, onCancelar }: {
  categorias: CategoriaFinanceira[];
  onCriado: () => void;
  onCancelar: () => void;
}) {
  const [tipo, setTipo] = useState<TipoLancamento>('RECEITA');
  const [categoriaId, setCategoriaId] = useState('');
  const [descricao, setDescricao] = useState('');
  const [valor, setValor] = useState('');
  const [dataCompetencia, setDataCompetencia] = useState(new Date().toISOString().slice(0, 10));
  const [status, setStatus] = useState<StatusLancamento>('REALIZADO');
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
        tipo, categoriaId, descricao, valor: Number(valor), dataCompetencia, status,
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
            Categoria
            <select className={styles.select} value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)} required>
              <option value="">{categoriasDoTipo.length === 0 ? 'Nenhuma categoria cadastrada' : 'Selecione…'}</option>
              {categoriasDoTipo.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
            {categoriasDoTipo.length === 0 && (
              <div className={styles.muted}>
                Nenhuma categoria de {tipo === 'RECEITA' ? 'receita' : 'despesa'} cadastrada — crie uma em "+ Nova categoria".
              </div>
            )}
          </label>
          <label className={styles.formField}>
            Status
            <select className={styles.select} value={status} onChange={(e) => setStatus(e.target.value as StatusLancamento)}>
              <option value="REALIZADO">Realizado</option>
              <option value="PREVISTO">Previsto</option>
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
            Data de competência
            <input className={styles.textInput} type="date" value={dataCompetencia} onChange={(e) => setDataCompetencia(e.target.value)} required />
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
