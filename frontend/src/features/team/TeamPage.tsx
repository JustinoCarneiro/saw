import { type FormEvent, useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../../shared/lib/apiClient';
import { Avatar } from '../../shared/components/Avatar';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { AreaPill, areaLabel, areaDotColor } from '../../shared/components/Pill';
import { Topbar } from '../../shared/components/Topbar';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Area, Colaborador, Modulo, PermissionMatrixRow } from '../../shared/lib/types';
import styles from './TeamPage.module.css';

const TEAM_COLUMNS = '1.6fr 1.5fr .9fr 1fr';
const MATRIX_COLUMNS = '1.6fr .8fr .8fr .8fr .9fr .9fr .8fr 1.1fr';
const MATRIX_MODULOS: Modulo[] = ['DASHBOARD', 'COMERCIAL', 'FINANCEIRO', 'MENTORADOS', 'CONTEUDOS', 'TIME', 'PAINEL_CONSOLIDADO'];

const AREA_LABEL: Record<Area, string> = {
  COMERCIAL: 'Comercial',
  MARKETING: 'Marketing',
  GESTAO_PERFORMANCE: 'Gestão de Performance',
  FUNDADOR: 'Fundador',
};

export function TeamPage() {
  const { user } = useAuth();
  const [colaboradores, setColaboradores] = useState<Colaborador[] | null>(null);
  const [matrix, setMatrix] = useState<PermissionMatrixRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

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
          <button className={styles.newButton} onClick={() => setCriando(true)} data-testid="novo-colaborador-botao">
            <span style={{ fontSize: 16 }}>+</span>Novo colaborador
          </button>
        </div>

        {criando && (
          <ColaboradorForm
            onSalvo={() => { setCriando(false); carregar(); }}
            onCancelar={() => setCriando(false)}
          />
        )}

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.layout}>
          <DataGrid columns={TEAM_COLUMNS} headers={['Colaborador', 'Área', 'Carteira (Ment.)', 'Conversões']}>
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
                <div className={styles.metric}>{c.carteira ?? '—'}</div>
                <div className={styles.metricGood}>{c.conversaoPct != null ? `${c.conversaoPct}%` : '—'}</div>
              </DataGridRow>
            ))}
          </DataGrid>

          <Card style={{ padding: 24, height: 'fit-content' }}>
            <div className={styles.profileBlock}>
              <Avatar name={user.nome} size={80} />
              <div className={styles.profileName}>{user.nome}</div>
              <AreaPill area={user.area ?? ''} />
            </div>

            <div className={styles.sectionTitle}>Permissões de acesso</div>
            <div className={styles.permList}>
              {MATRIX_MODULOS.map((m) => {
                const has = user.modulosPermitidos.includes(m);
                return (
                  <div key={m} className={styles.permRow} style={{ opacity: has ? 1 : 0.4 }}>
                    <span style={{ color: has ? 'var(--success)' : 'var(--text-faint)' }}>{has ? '✓' : '—'}</span>
                    {MODULO_LABEL[m]}
                  </div>
                );
              })}
            </div>
          </Card>
        </div>

        <Card style={{ marginTop: 16, overflow: 'hidden' }} className={styles.matrixCard}>
          <div className={styles.matrixHeader}>
            <div className={styles.sectionTitle}>Matriz de permissões por área</div>
            <div className={styles.matrixSubtitle}>
              Cada colaborador acessa só as telas da própria área. O Fundador acessa tudo.
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
                    {row.modulosPermitidos.includes(m) ? '✓' : '—'}
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

const MODULO_LABEL: Record<Modulo, string> = {
  DASHBOARD: 'Dashboard',
  COMERCIAL: 'Comercial',
  FINANCEIRO: 'Financeiro',
  MENTORADOS: 'Mentorados',
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
