import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import { Tooltip } from '../../shared/components/Tooltip';
import type { CaixaMensalResponse, ContaBancaria, TransferenciaBancaria } from '../../shared/lib/types';
import { formatBRL } from '../../shared/lib/format';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import styles from './CaixaPage.module.css';

const TRANSFERENCIA_COLUMNS = '1fr 1fr .3fr 1fr .9fr 1.4fr';

function primeiroDiaDoMes(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

function ultimoDiaDoMes(): string {
  const d = new Date();
  const ultimo = new Date(d.getFullYear(), d.getMonth() + 1, 0);
  return ultimo.toISOString().slice(0, 10);
}

// "Caixa do mês: Inicial, saldo por banco, Final" + "Transferências Entre Contas" (change request
// pós-MVP, E14, reunião 17/07/2026) — entrada manual do Admin, mesmo critério da planilha real
// "DRE Financeira Saw" (ver backend CaixaMensalService).
export function CaixaPage() {
  const now = new Date();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [caixa, setCaixa] = useState<CaixaMensalResponse | null>(null);
  const [contas, setContas] = useState<ContaBancaria[]>([]);
  const [transferencias, setTransferencias] = useState<TransferenciaBancaria[] | null>(null);
  const [de, setDe] = useState(primeiroDiaDoMes());
  const [ate, setAte] = useState(ultimoDiaDoMes());
  const [error, setError] = useState<string | null>(null);
  const [showContaForm, setShowContaForm] = useState(false);
  const [showPosicaoForm, setShowPosicaoForm] = useState(false);
  const [showTransferenciaForm, setShowTransferenciaForm] = useState(false);

  const carregarCaixa = () => {
    setCaixa(null);
    apiClient
      .get<CaixaMensalResponse>('/admin/financeiro/caixa', { params: { ano, mes } })
      .then((res) => setCaixa(res.data))
      .catch(() => setError('Não foi possível carregar o caixa do mês.'));
  };

  const carregarContas = () => {
    apiClient
      .get<ContaBancaria[]>('/admin/financeiro/contas-bancarias')
      .then((res) => setContas(res.data))
      .catch(() => setError('Não foi possível carregar as contas bancárias.'));
  };

  const carregarTransferencias = () => {
    setTransferencias(null);
    apiClient
      .get<TransferenciaBancaria[]>('/admin/financeiro/transferencias', { params: { de, ate } })
      .then((res) => setTransferencias(res.data))
      .catch(() => setError('Não foi possível carregar as transferências.'));
  };

  useEffect(carregarCaixa, [ano, mes]);
  useEffect(carregarContas, []);
  useEffect(carregarTransferencias, [de, ate]);

  return (
    <div>
      <div className={styles.toolbar}>
        <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
        <div style={{ display: 'flex', gap: 12 }}>
          <button className={styles.outlineButton} onClick={() => setShowContaForm((v) => !v)}>
            <span style={{ fontSize: 16 }}>+</span>Nova conta bancária
          </button>
          <button className={styles.newButton} onClick={() => setShowPosicaoForm((v) => !v)}>
            <span style={{ fontSize: 16 }}>+</span>Registrar posição do mês
          </button>
        </div>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {showContaForm && (
        <NovaContaBancariaForm
          onCriado={() => { setShowContaForm(false); carregarContas(); }}
          onCancelar={() => setShowContaForm(false)}
        />
      )}

      {showPosicaoForm && (
        <RegistrarPosicaoForm
          contas={contas}
          anoInicial={ano}
          mesInicial={mes}
          onRegistrado={() => { setShowPosicaoForm(false); carregarCaixa(); }}
          onCancelar={() => setShowPosicaoForm(false)}
        />
      )}

      {caixa && (
        <>
          <div className={styles.kpis}>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Soma do saldo inicial de todas as contas bancárias registradas pra este mês.">
                  Caixa inicial ({caixa.contas.length} conta(s))
                </Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(caixa.totalInicial)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Soma do saldo final de todas as contas bancárias registradas pra este mês.">Caixa final</Tooltip>
              </div>
              <div className={styles.kpiValue}>{formatBRL(caixa.totalFinal)}</div>
            </Card>
            <Card style={{ padding: 18 }}>
              <div className={styles.kpiLabel}>
                <Tooltip text="Diferença entre Caixa final e Caixa inicial do mês.">Variação no mês</Tooltip>
              </div>
              <div
                className={styles.kpiValue}
                style={{ color: caixa.totalFinal - caixa.totalInicial >= 0 ? 'var(--success)' : 'var(--danger)' }}
              >
                {formatBRL(caixa.totalFinal - caixa.totalInicial)}
              </div>
            </Card>
          </div>

          <Card style={{ padding: '20px 22px' }}>
            <div className={styles.sectionTitle}>
              <Tooltip text="Posição inicial e final de cada conta bancária, registrada manualmente pelo time.">Saldo por banco</Tooltip>
            </div>
            {caixa.contas.length === 0 && (
              <div className={styles.muted}>Nenhuma posição registrada para este mês ainda.</div>
            )}
            <div className={styles.contasList}>
              {caixa.contas.map((c) => (
                <div key={c.contaBancariaId} className={styles.contaRow}>
                  <span className={styles.strong}>{c.contaBancariaNome}</span>
                  <span className={styles.muted}>
                    Inicial: <span className={styles.valor}>{formatBRL(c.saldoInicial)}</span>
                  </span>
                  <span className={styles.muted}>
                    Final: <span className={styles.valor}>{formatBRL(c.saldoFinal)}</span>
                  </span>
                </div>
              ))}
            </div>
          </Card>
        </>
      )}

      <Card style={{ padding: '20px 22px', marginTop: 16 }}>
        <div className={styles.sectionHeader}>
          <div className={styles.sectionTitle} style={{ marginBottom: 0 }}>
            <Tooltip text="Movimentações internas entre contas bancárias da SAW — não é receita nem despesa, só remanejamento de saldo.">Transferências entre contas</Tooltip>
          </div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <input type="date" className={styles.textInput} value={de} onChange={(e) => setDe(e.target.value)} />
            <input type="date" className={styles.textInput} value={ate} onChange={(e) => setAte(e.target.value)} />
            <button className={styles.outlineButton} onClick={() => setShowTransferenciaForm((v) => !v)}>
              <span style={{ fontSize: 16 }}>+</span>Nova transferência
            </button>
          </div>
        </div>

        {showTransferenciaForm && (
          <NovaTransferenciaForm
            contas={contas}
            onCriado={() => { setShowTransferenciaForm(false); carregarTransferencias(); }}
            onCancelar={() => setShowTransferenciaForm(false)}
          />
        )}

        <DataGrid columns={TRANSFERENCIA_COLUMNS} headers={['Origem', 'Destino', '', 'Data', 'Valor', 'Descrição']}>
          {transferencias === null && !error && <div className={styles.loading}>Carregando…</div>}
          {transferencias?.length === 0 && <div className={styles.loading}>Nenhuma transferência neste período.</div>}
          {transferencias?.map((t) => (
            <DataGridRow key={t.id} columns={TRANSFERENCIA_COLUMNS}>
              <div className={styles.strong}>{t.contaOrigemNome}</div>
              <div className={styles.strong}>{t.contaDestinoNome}</div>
              <div className={styles.muted}>→</div>
              <div className={styles.muted}>{new Date(t.data + 'T00:00:00').toLocaleDateString('pt-BR')}</div>
              <div className={styles.valor}>{formatBRL(t.valor)}</div>
              <div className={styles.muted}>{t.descricao ?? '—'}</div>
            </DataGridRow>
          ))}
        </DataGrid>
      </Card>
    </div>
  );
}

function NovaContaBancariaForm({ onCriado, onCancelar }: { onCriado: () => void; onCancelar: () => void }) {
  const [nome, setNome] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/financeiro/contas-bancarias', { nome });
      onCriado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível criar a conta bancária.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Nome da conta (ex.: Itaú, Infinity Pay)
            <input className={styles.textInput} value={nome} onChange={(e) => setNome(e.target.value)} required />
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

function RegistrarPosicaoForm({ contas, anoInicial, mesInicial, onRegistrado, onCancelar }: {
  contas: ContaBancaria[];
  anoInicial: number;
  mesInicial: number;
  onRegistrado: () => void;
  onCancelar: () => void;
}) {
  const [contaBancariaId, setContaBancariaId] = useState('');
  const [ano, setAno] = useState(anoInicial);
  const [mes, setMes] = useState(mesInicial);
  const [saldoInicial, setSaldoInicial] = useState('');
  const [saldoFinal, setSaldoFinal] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!contaBancariaId) {
      setError('Selecione uma conta bancária.');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.put('/admin/financeiro/caixa', {
        contaBancariaId, ano, mes, saldoInicial: Number(saldoInicial), saldoFinal: Number(saldoFinal),
      });
      onRegistrado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível registrar a posição de caixa.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Conta bancária
            <select className={styles.select} value={contaBancariaId} onChange={(e) => setContaBancariaId(e.target.value)} required>
              <option value="">{contas.length === 0 ? 'Nenhuma conta cadastrada' : 'Selecione…'}</option>
              {contas.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Ano
            <input className={styles.textInput} type="number" value={ano} onChange={(e) => setAno(Number(e.target.value))} required />
          </label>
          <label className={styles.formField}>
            Mês
            <input className={styles.textInput} type="number" min={1} max={12} value={mes} onChange={(e) => setMes(Number(e.target.value))} required />
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Saldo inicial (R$)
            <input className={styles.textInput} type="number" step="0.01" value={saldoInicial} onChange={(e) => setSaldoInicial(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Saldo final (R$)
            <input className={styles.textInput} type="number" step="0.01" value={saldoFinal} onChange={(e) => setSaldoFinal(e.target.value)} required />
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Registrar posição'}
          </button>
        </div>
      </form>
    </Card>
  );
}

function NovaTransferenciaForm({ contas, onCriado, onCancelar }: {
  contas: ContaBancaria[];
  onCriado: () => void;
  onCancelar: () => void;
}) {
  const [contaOrigemId, setContaOrigemId] = useState('');
  const [contaDestinoId, setContaDestinoId] = useState('');
  const [valor, setValor] = useState('');
  const [data, setData] = useState(new Date().toISOString().slice(0, 10));
  const [descricao, setDescricao] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!contaOrigemId || !contaDestinoId) {
      setError('Selecione conta de origem e destino.');
      return;
    }
    if (contaOrigemId === contaDestinoId) {
      setError('Conta de origem e destino não podem ser a mesma.');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient.post('/admin/financeiro/transferencias', {
        contaOrigemId, contaDestinoId, valor: Number(valor), data, descricao: descricao || null,
      });
      onCriado();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível registrar a transferência.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Conta de origem
            <select className={styles.select} value={contaOrigemId} onChange={(e) => setContaOrigemId(e.target.value)} required>
              <option value="">Selecione…</option>
              {contas.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Conta de destino
            <select className={styles.select} value={contaDestinoId} onChange={(e) => setContaDestinoId(e.target.value)} required>
              <option value="">Selecione…</option>
              {contas.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Valor (R$)
            <input className={styles.textInput} type="number" min="0.01" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Data
            <input className={styles.textInput} type="date" value={data} onChange={(e) => setData(e.target.value)} required />
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Descrição (opcional)
            <input className={styles.textInput} placeholder="Ex.: Empréstimo interno" value={descricao} onChange={(e) => setDescricao(e.target.value)} />
          </label>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.newButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Registrar transferência'}
          </button>
        </div>
      </form>
    </Card>
  );
}
