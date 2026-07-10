import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Aviso, CategoriaAviso, Plano } from '../../shared/lib/types';
import styles from './ConteudosPage.module.css';

const COLUMNS = '2fr 1fr 1fr 1.6fr';

const CATEGORIA_LABEL: Record<CategoriaAviso, string> = {
  GERAL: 'Geral', MENTORIAS: 'Mentorias', MATERIAIS: 'Materiais', EVENTOS: 'Eventos',
};

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito', BASICO: 'Básico', ESSENCIAL: 'Essencial', PROFISSIONAL: 'Profissional',
};

function formatarData(iso: string): string {
  return new Date(iso).toLocaleDateString('pt-BR');
}

export function AvisosAdminPage() {
  const [avisos, setAvisos] = useState<Aviso[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

  const carregar = () => {
    setAvisos(null);
    apiClient.get<Aviso[]>('/admin/avisos')
      .then((res) => setAvisos(res.data))
      .catch(() => setError('Não foi possível carregar os avisos.'));
  };

  useEffect(carregar, []);

  return (
    <div>
      <div className={styles.toolbar}>
        <button className={styles.newButton} onClick={() => setCriando(true)}>
          <span style={{ fontSize: 16 }}>+</span>Novo aviso
        </button>
      </div>

      {criando && <AvisoForm onSalvo={() => { setCriando(false); carregar(); }} onCancelar={() => setCriando(false)} />}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Título', 'Categoria', 'Plano mínimo', 'Publicado em']}>
        {avisos === null && !error && <div className={styles.loading}>Carregando…</div>}
        {avisos?.length === 0 && <div className={styles.loading}>Nenhum aviso publicado ainda.</div>}
        {avisos?.map((a) => (
          <DataGridRow key={a.id} columns={COLUMNS} testId={`aviso-row-${a.id}`}>
            <div className={styles.strong}>{a.titulo}</div>
            <div><Pill bg="var(--elevated)" color="var(--text-soft)">{CATEGORIA_LABEL[a.categoria]}</Pill></div>
            <div className={styles.muted}>{PLANO_LABEL[a.planoMinimo]}</div>
            <div className={styles.muted}>{formatarData(a.criadoEm)}</div>
          </DataGridRow>
        ))}
      </DataGrid>
    </div>
  );
}

function AvisoForm({ onSalvo, onCancelar }: { onSalvo: () => void; onCancelar: () => void }) {
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [categoria, setCategoria] = useState<CategoriaAviso>('GERAL');
  const [planoMinimo, setPlanoMinimo] = useState<Plano>('GRATUITO');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/avisos', { titulo, descricao, categoria, planoMinimo });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível publicar o aviso. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Novo aviso</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.formField}>
          Título
          <input className={styles.textInput} value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
        </label>
        <label className={styles.formField}>
          Descrição
          <textarea className={styles.textarea} value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
        </label>
        <div className={styles.formRow}>
          <label className={styles.formField}>
            Categoria
            <select className={styles.select} value={categoria} onChange={(e) => setCategoria(e.target.value as CategoriaAviso)}>
              {(Object.keys(CATEGORIA_LABEL) as CategoriaAviso[]).map((c) => (
                <option key={c} value={c}>{CATEGORIA_LABEL[c]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Visível a partir do plano
            <select className={styles.select} value={planoMinimo} onChange={(e) => setPlanoMinimo(e.target.value as Plano)}>
              {(Object.keys(PLANO_LABEL) as Plano[]).map((p) => (
                <option key={p} value={p}>{PLANO_LABEL[p]}</option>
              ))}
            </select>
          </label>
        </div>
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.actionButton} disabled={submitting}>
            {submitting ? 'Publicando…' : 'Publicar'}
          </button>
        </div>
      </form>
    </Card>
  );
}
