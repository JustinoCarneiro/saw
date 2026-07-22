import { type FormEvent, useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../../shared/lib/apiClient';
import { Avatar } from '../../shared/components/Avatar';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { ICON_PROPS } from '../../shared/components/iconProps';
import { PeriodoPicker } from '../../shared/components/PeriodoPicker';
import { AreaPill, areaLabel, areaDotColor } from '../../shared/components/Pill';
import { Tooltip } from '../../shared/components/Tooltip';
import { Topbar } from '../../shared/components/Topbar';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type {
  Area,
  CategoriaFinanceira,
  Colaborador,
  DesempenhoColaborador,
  Modulo,
  PermissionMatrixRow,
} from '../../shared/lib/types';
import styles from './TeamPage.module.css';

// Pedido do Marcos (22/07/2026) — "pagar qualquer colaborador de qualquer área (salário, 13º,
// comissão etc.), mais descritivo que um lançamento manual". Não é um conceito do backend: gera
// um POST comum em /admin/financeiro/lancamentos (mesmo endpoint de "Novo lançamento" no
// Financeiro), só que a descrição já nasce padronizada (tipo + nome da pessoa + período) em vez
// do Admin digitar isso de cabeça toda vez. Existe só aqui no frontend de propósito — não precisa
// de enum novo no banco nem de decidir se `team` pode depender de `financeiro`.
type TipoPagamentoColaborador = 'SALARIO' | 'DECIMO_TERCEIRO' | 'COMISSAO' | 'OUTRO';

const TIPO_PAGAMENTO_LABEL: Record<TipoPagamentoColaborador, string> = {
  SALARIO: 'Salário',
  DECIMO_TERCEIRO: '13º salário',
  COMISSAO: 'Comissão',
  OUTRO: 'Pagamento',
};

const TEAM_COLUMNS = '1.6fr 1.5fr 1fr';
const DESEMPENHO_COLUMNS = '1.6fr 1.5fr 1.2fr 1fr 1fr 1.4fr';
const MATRIX_COLUMNS = '1.6fr .8fr .8fr .8fr .9fr .9fr .8fr 1.1fr';
const MATRIX_MODULOS: Modulo[] = ['DASHBOARD', 'COMERCIAL', 'FINANCEIRO', 'MENTORADOS', 'CONTEUDOS', 'TIME', 'PAINEL_CONSOLIDADO'];

// Achado de UX: '✓' Unicode como indicador de permissão concedida destoava do traço linear
// (ICON_PROPS) usado no resto do app. '—' continua texto de propósito: já é a convenção deste
// arquivo pra "sem valor" (linhas de métrica logo abaixo), então "não permitido" reaproveita o
// mesmo sinal em vez de inventar um segundo símbolo pra a mesma ideia.
const ICONE_PERMITIDO = (
  <svg {...ICON_PROPS} width={14} height={14}>
    <path d="M4 12l5 5L20 6" />
  </svg>
);

const AREA_LABEL: Record<Area, string> = {
  COMERCIAL: 'Comercial',
  MARKETING: 'Marketing',
  GESTAO_PERFORMANCE: 'Gestão de Performance',
  ADMIN: 'Admin',
};

export function TeamPage() {
  const { user } = useAuth();
  const [colaboradores, setColaboradores] = useState<Colaborador[] | null>(null);
  const [matrix, setMatrix] = useState<PermissionMatrixRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [categorias, setCategorias] = useState<CategoriaFinanceira[]>([]);
  const [pagando, setPagando] = useState(false);

  const now = new Date();
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [desempenho, setDesempenho] = useState<DesempenhoColaborador[] | null>(null);
  const [erroDesempenho, setErroDesempenho] = useState<string | null>(null);

  const carregar = () => {
    Promise.all([
      apiClient.get<Colaborador[]>('/admin/team'),
      apiClient.get<PermissionMatrixRow[]>('/admin/team/permission-matrix'),
    ])
      .then(([teamRes, matrixRes]) => {
        setColaboradores(teamRes.data);
        setMatrix(matrixRes.data);
      })
      .catch(() => setError('Não foi possível carregar o time.'));
  };

  useEffect(carregar, []);

  useEffect(() => {
    apiClient.get<CategoriaFinanceira[]>('/admin/financeiro/categorias')
      .then((res) => setCategorias(res.data))
      .catch(() => setError('Não foi possível carregar as subcategorias financeiras.'));
  }, []);

  useEffect(() => {
    setDesempenho(null);
    apiClient
      .get<DesempenhoColaborador[]>('/admin/team/desempenho', { params: { ano, mes } })
      .then((res) => setDesempenho(res.data))
      .catch(() => setErroDesempenho('Não foi possível carregar o desempenho do time.'));
  }, [ano, mes]);

  if (!user) return null;

  return (
    <div className={styles.page}>
      <Topbar
        title="Gestão de Time"
        subtitle="Colaboradores da SAW, áreas e permissões de acesso."
        userName={user.nome}
        userRole={areaLabel(user.area ?? '')}
      />

      <div className={styles.content}>
        <div className={styles.toolbar}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <CsvImportExport
              exportUrl="/admin/team/export"
              exportFilename="colaboradores.csv"
              importUrl="/admin/team/import"
              onImportado={carregar}
            />
            <button className={styles.newButton} onClick={() => setCriando(true)} data-testid="novo-colaborador-botao">
              <span style={{ fontSize: 16 }}>+</span>Novo colaborador
            </button>
            <button className={styles.newButton} onClick={() => setPagando(true)} data-testid="novo-pagamento-botao">
              <span style={{ fontSize: 16 }}>+</span>Novo pagamento
            </button>
          </div>
        </div>

        {criando && (
          <ColaboradorForm
            onSalvo={() => { setCriando(false); carregar(); }}
            onCancelar={() => setCriando(false)}
          />
        )}

        {pagando && colaboradores && (
          <PagamentoColaboradorForm
            colaboradores={colaboradores}
            categorias={categorias}
            onSalvo={() => setPagando(false)}
            onCancelar={() => setPagando(false)}
          />
        )}

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.layout}>
          <DataGrid columns={TEAM_COLUMNS} headers={['Colaborador', 'Área', 'Carteira (Ment.)']}>
            {colaboradores === null && !error && <div className={styles.loading}>Carregando…</div>}
            {colaboradores?.map((c) => (
              <DataGridRow key={c.id} columns={TEAM_COLUMNS} testId={`colaborador-row-${c.id}`}>
                <div className={styles.person}>
                  <Avatar name={c.nome} size={36} />
                  <div className={styles.personText}>
                    <div className={styles.personName}>{c.nome}</div>
                    <div className={styles.personEmail}>{c.email}</div>
                  </div>
                </div>
                <div>
                  <AreaPill area={c.area} />
                </div>
                <div className={styles.metric}>{c.carteira}</div>
              </DataGridRow>
            ))}
          </DataGrid>

          <Card style={{ padding: 24, height: 'fit-content' }}>
            <div className={styles.profileBlock}>
              <Avatar name={user.nome} size={80} />
              <div className={styles.profileName}>{user.nome}</div>
              <AreaPill area={user.area ?? ''} />
            </div>

            <div className={styles.sectionTitle}>
              <Tooltip text="Módulos que sua área pode acessar (RBAC). Fundador acessa tudo, as demais áreas só o que está marcado abaixo.">Permissões de acesso</Tooltip>
            </div>
            <div className={styles.permList}>
              {MATRIX_MODULOS.map((m) => {
                const has = user.modulosPermitidos.includes(m);
                return (
                  <div key={m} className={styles.permRow} style={{ opacity: has ? 1 : 0.4 }}>
                    <span style={{ color: has ? 'var(--success)' : 'var(--text-faint)' }}>{has ? ICONE_PERMITIDO : '—'}</span>
                    {MODULO_LABEL[m]}
                  </div>
                );
              })}
            </div>
          </Card>
        </div>

        <Card style={{ marginTop: 16, overflow: 'hidden' }}>
          <div className={styles.matrixHeader}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
              <div>
                <div className={styles.sectionTitle} style={{ marginBottom: 2 }}>Desempenho do Time</div>
                <div className={styles.matrixSubtitle}>
                  Mentorias realizadas por todo mundo; meta x realizado de fechamentos só pra quem já tem meta comercial no período.
                </div>
              </div>
              <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
            </div>
          </div>
          {erroDesempenho && <div className={styles.error} style={{ padding: '0 22px 12px' }}>{erroDesempenho}</div>}
          <DataGrid
            columns={DESEMPENHO_COLUMNS}
            headers={['Colaborador', 'Área', 'Mentorias realizadas', 'Meta', 'Realizado', '% atingido']}
          >
            {desempenho === null && !erroDesempenho && <div className={styles.loading}>Carregando…</div>}
            {desempenho?.map((d) => (
              <DataGridRow key={d.id} columns={DESEMPENHO_COLUMNS} testId={`desempenho-row-${d.id}`}>
                <div className={styles.personName}>{d.nome}</div>
                <div>
                  <AreaPill area={d.area} />
                </div>
                <div className={styles.metric}>{d.mentoriasRealizadas}</div>
                <div className={styles.metric} data-testid="meta-fechamentos">{d.metaFechamentos ?? '—'}</div>
                <div className={styles.metric} data-testid="fechamentos-realizados">{d.fechamentosRealizados ?? '—'}</div>
                <div className={styles.metricGood}>
                  {d.pctAtingidoFechamentos != null ? `${d.pctAtingidoFechamentos.toFixed(1)}%` : '—'}
                </div>
              </DataGridRow>
            ))}
          </DataGrid>
        </Card>

        <Card style={{ marginTop: 16, overflow: 'hidden' }} className={styles.matrixCard}>
          <div className={styles.matrixHeader}>
            <div className={styles.sectionTitle}>Matriz de permissões por área</div>
            <div className={styles.matrixSubtitle}>
              Cada colaborador acessa só as telas da própria área. O Admin acessa tudo.
            </div>
          </div>
          <div className={styles.matrixScroll}>
            <div className={styles.matrixGrid} style={{ gridTemplateColumns: MATRIX_COLUMNS }}>
              <div className={styles.matrixHeadCell}>Área</div>
              {MATRIX_MODULOS.map((m) => (
                <div key={m} className={styles.matrixHeadCell}>
                  {MODULO_LABEL[m]}
                </div>
              ))}
            </div>
            {matrix?.map((row) => (
              <div key={row.area} className={styles.matrixGrid} style={{ gridTemplateColumns: MATRIX_COLUMNS }}>
                <div className={styles.matrixAreaCell}>
                  <span className={styles.dot} style={{ background: areaDotColor(row.area) }} />
                  {areaLabel(row.area)}
                </div>
                {MATRIX_MODULOS.map((m) => (
                  <div
                    key={m}
                    className={styles.matrixMark}
                    style={{ color: row.modulosPermitidos.includes(m) ? 'var(--success)' : 'var(--text-faint)' }}
                  >
                    {row.modulosPermitidos.includes(m) ? ICONE_PERMITIDO : '—'}
                  </div>
                ))}
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

// M28 — Modulo.MENTORADOS renomeado pra "Gestão de Performance" na UI (rename só de label, ver
// Sidebar.tsx/MentoradosShell.tsx).
const MODULO_LABEL: Record<Modulo, string> = {
  DASHBOARD: 'Dashboard',
  COMERCIAL: 'Comercial',
  FINANCEIRO: 'Financeiro',
  MENTORADOS: 'Gestão de Performance',
  CONTEUDOS: 'Conteúdos',
  TIME: 'Time',
  PAINEL_CONSOLIDADO: 'Painel Consolidado',
};

// H15.1 (M19) — backend/POST /api/v1/admin/team já existiam desde antes desta leva
// (TeamService.criar, testado), só faltava o formulário chamando o endpoint.
function ColaboradorForm({ onSalvo, onCancelar }: { onSalvo: () => void; onCancelar: () => void }) {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [area, setArea] = useState<Area>('COMERCIAL');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/team', { nome, email, senha, area });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível cadastrar o colaborador. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Novo colaborador</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Nome
            <input className={styles.textInput} value={nome} onChange={(e) => setNome(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Área
            <select className={styles.select} value={area} onChange={(e) => setArea(e.target.value as Area)}>
              {(Object.keys(AREA_LABEL) as Area[]).map((a) => (
                <option key={a} value={a}>{AREA_LABEL[a]}</option>
              ))}
            </select>
          </label>
        </div>
        <label className={styles.formField}>
          E-mail
          <input className={styles.textInput} type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label className={styles.formField}>
          Senha (mínimo 8 caracteres)
          <input className={styles.textInput} type="password" value={senha} onChange={(e) => setSenha(e.target.value)} minLength={8} autoComplete="new-password" required />
        </label>
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </Card>
  );
}

// Pedido do Marcos (22/07/2026) — "pagar qualquer colaborador de qualquer área, mais descritivo
// que hoje": Admin escolhe colaborador + tipo de pagamento + subcategoria + valor + período, e a
// descrição do lançamento nasce padronizada (nunca digitada solta). Só DESPESA na subcategoria —
// pagamento a colaborador nunca é receita.
function PagamentoColaboradorForm({ colaboradores, categorias, onSalvo, onCancelar }: {
  colaboradores: Colaborador[];
  categorias: CategoriaFinanceira[];
  onSalvo: () => void;
  onCancelar: () => void;
}) {
  const now = new Date();
  const [colaboradorId, setColaboradorId] = useState('');
  const [tipoPagamento, setTipoPagamento] = useState<TipoPagamentoColaborador>('SALARIO');
  const [categoriaId, setCategoriaId] = useState('');
  const [valor, setValor] = useState('');
  const [ano, setAno] = useState(now.getFullYear());
  const [mes, setMes] = useState(now.getMonth() + 1);
  const [jaPago, setJaPago] = useState(true);
  const [observacao, setObservacao] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const categoriasDespesa = categorias.filter((c) => c.tipo === 'DESPESA');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const colaborador = colaboradores.find((c) => c.id === colaboradorId);
    if (!colaborador) {
      setError('Selecione um colaborador.');
      return;
    }
    if (!categoriaId) {
      setError('Selecione uma subcategoria.');
      return;
    }
    setSubmitting(true);
    const periodo = `${String(mes).padStart(2, '0')}/${ano}`;
    const descricao = `${TIPO_PAGAMENTO_LABEL[tipoPagamento]} — ${colaborador.nome} (${periodo})`
      + (observacao.trim() ? ` — ${observacao.trim()}` : '');
    // Último dia do mês/ano escolhido — mesma convenção de "competência" que o resto do
    // Financeiro usa pra um fato datado só por período, não por dia exato (ver PeriodoPicker).
    const dataCompetencia = new Date(ano, mes, 0).toISOString().slice(0, 10);
    try {
      await apiClient.post('/admin/financeiro/lancamentos', {
        tipo: 'DESPESA',
        categoriaId,
        descricao,
        valor: Number(valor),
        status: jaPago ? 'REALIZADO' : 'PREVISTO',
        dataCompetencia,
        dataVencimento: jaPago ? null : dataCompetencia,
      });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível registrar o pagamento. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }} testId="pagamento-colaborador-form">
      <div className={styles.formTitle}>Novo pagamento</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Colaborador
            <select className={styles.select} value={colaboradorId} onChange={(e) => setColaboradorId(e.target.value)} required>
              <option value="">Selecione…</option>
              {colaboradores.map((c) => (
                <option key={c.id} value={c.id}>{c.nome} ({AREA_LABEL[c.area]})</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Tipo de pagamento
            <select className={styles.select} value={tipoPagamento}
                    onChange={(e) => setTipoPagamento(e.target.value as TipoPagamentoColaborador)}>
              {Object.entries(TIPO_PAGAMENTO_LABEL).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField} style={{ flex: 2 }}>
            Subcategoria (Financeiro)
            <select className={styles.select} value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)} required>
              <option value="">{categoriasDespesa.length === 0 ? 'Nenhuma subcategoria de despesa cadastrada' : 'Selecione…'}</option>
              {categoriasDespesa.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Valor (R$)
            <input className={styles.textInput} type="number" min="0.01" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
          </label>
          <div className={styles.formField} style={{ flex: '0 0 auto' }}>
            Período
            <PeriodoPicker ano={ano} mes={mes} onChange={(a, m) => { setAno(a); setMes(m); }} />
          </div>
          <label className={styles.formField}>
            Já foi pago?
            <select className={styles.select} value={jaPago ? 'sim' : 'nao'} onChange={(e) => setJaPago(e.target.value === 'sim')}>
              <option value="sim">Sim (já aconteceu)</option>
              <option value="nao">Não (previsto)</option>
            </select>
          </label>
        </div>
        <label className={styles.formField}>
          Observação (opcional)
          <input className={styles.textInput} value={observacao} onChange={(e) => setObservacao(e.target.value)} />
        </label>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Registrar pagamento'}
          </button>
        </div>
      </form>
    </Card>
  );
}
