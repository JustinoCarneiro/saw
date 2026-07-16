import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { CsvImportExport } from '../../shared/components/CsvImportExport';
import { DataGrid, DataGridRow } from '../../shared/components/DataGrid';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Conteudo, Plano, TipoConteudo } from '../../shared/lib/types';
import styles from './ConteudosPage.module.css';

const COLUMNS = '2fr 1fr 1fr 1fr 1.6fr';

const TIPO_LABEL: Record<TipoConteudo, string> = {
  DOCUMENTO: 'Documento', VIDEO: 'Vídeo', PLANILHA: 'Planilha', APRESENTACAO: 'Apresentação', OUTRO: 'Outro',
};

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito', BASICO: 'Básico', ESSENCIAL: 'Essencial', PROFISSIONAL: 'Profissional',
};

export function ConteudosPage() {
  const [tipo, setTipo] = useState<TipoConteudo | ''>('');
  const [conteudos, setConteudos] = useState<Conteudo[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);

  const carregar = () => {
    setConteudos(null);
    apiClient
      .get<Conteudo[]>('/admin/conteudos', { params: { tipo: tipo || undefined } })
      .then((res) => setConteudos(res.data))
      .catch(() => setError('Não foi possível carregar os conteúdos.'));
  };

  useEffect(carregar, [tipo]);

  async function alternarPublicacao(c: Conteudo) {
    await apiClient.patch(`/admin/conteudos/${c.id}/${c.publicado ? 'despublicar' : 'publicar'}`);
    carregar();
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <select className={styles.select} value={tipo} onChange={(e) => setTipo(e.target.value as TipoConteudo | '')}>
          <option value="">Todos os tipos</option>
          {(Object.keys(TIPO_LABEL) as TipoConteudo[]).map((t) => (
            <option key={t} value={t}>{TIPO_LABEL[t]}</option>
          ))}
        </select>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <CsvImportExport
            exportUrl="/admin/conteudos/export"
            exportParams={{ tipo: tipo || undefined }}
            exportFilename="conteudos.csv"
            importUrl="/admin/conteudos/import"
            onImportado={carregar}
          />
          <button className={styles.newButton} onClick={() => setCriando(true)}>
            <span style={{ fontSize: 16 }}>+</span>Novo conteúdo
          </button>
        </div>
      </div>

      {criando && <ConteudoForm onSalvo={() => { setCriando(false); carregar(); }} onCancelar={() => setCriando(false)} />}

      {error && <div className={styles.error}>{error}</div>}

      <DataGrid columns={COLUMNS} headers={['Título', 'Tipo', 'Plano mínimo', 'Status', 'Ações']}>
        {conteudos === null && !error && <div className={styles.loading}>Carregando…</div>}
        {conteudos?.length === 0 && <div className={styles.loading}>Nenhum conteúdo encontrado.</div>}
        {conteudos?.map((c) => (
          <DataGridRow key={c.id} columns={COLUMNS}>
            <div className={styles.strong}>{c.titulo}</div>
            <div className={styles.muted}>{TIPO_LABEL[c.tipo]}</div>
            <div className={styles.muted}>{PLANO_LABEL[c.planoMinimo]}</div>
            <div>
              <Pill bg={c.publicado ? 'var(--success-bg)' : 'var(--line)'} color={c.publicado ? 'var(--success)' : 'var(--text-soft)'}>
                {c.publicado ? 'Publicado' : 'Rascunho'}
              </Pill>
            </div>
            <div className={styles.acoes}>
              <button className={styles.actionButton} onClick={() => alternarPublicacao(c)}>
                {c.publicado ? 'Despublicar' : 'Publicar'}
              </button>
            </div>
          </DataGridRow>
        ))}
      </DataGrid>
    </div>
  );
}

function ConteudoForm({ onSalvo, onCancelar }: { onSalvo: () => void; onCancelar: () => void }) {
  const [titulo, setTitulo] = useState('');
  const [tipo, setTipo] = useState<TipoConteudo>('DOCUMENTO');
  const [url, setUrl] = useState('');
  const [planoMinimo, setPlanoMinimo] = useState<Plano>('GRATUITO');
  const [duracaoMinutos, setDuracaoMinutos] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/admin/conteudos', {
        titulo, tipo, url, planoMinimo,
        duracaoMinutos: duracaoMinutos ? Number(duracaoMinutos) : null,
      });
      onSalvo();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar o conteúdo.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 20, marginBottom: 16 }}>
      <div className={styles.formTitle}>Novo conteúdo</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            Título
            <input className={styles.textInput} value={titulo} onChange={(e) => setTitulo(e.target.value)} required />
          </label>
          <label className={styles.formField}>
            Tipo
            <select className={styles.select} value={tipo} onChange={(e) => setTipo(e.target.value as TipoConteudo)}>
              {(Object.keys(TIPO_LABEL) as TipoConteudo[]).map((t) => (
                <option key={t} value={t}>{TIPO_LABEL[t]}</option>
              ))}
            </select>
          </label>
          <label className={styles.formField}>
            Plano mínimo
            <select className={styles.select} value={planoMinimo} onChange={(e) => setPlanoMinimo(e.target.value as Plano)}>
              {(Object.keys(PLANO_LABEL) as Plano[]).map((p) => (
                <option key={p} value={p}>{PLANO_LABEL[p]}</option>
              ))}
            </select>
          </label>
        </div>
        <div className={styles.formRow}>
          <label className={styles.formField} style={{ flex: 2 }}>
            URL
            <input className={styles.textInput} value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://..." required />
          </label>
          {tipo === 'VIDEO' && (
            <label className={styles.formField}>
              Duração (min, opcional)
              <input
                className={styles.textInput}
                type="number"
                min="1"
                value={duracaoMinutos}
                onChange={(e) => setDuracaoMinutos(e.target.value)}
                placeholder="Ex.: 12"
              />
            </label>
          )}
        </div>
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
