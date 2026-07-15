import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { ICON_PROPS } from '../../shared/components/iconProps';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { TIPO_ICONE } from '../../shared/lib/materialDisplay';
import type { Conteudo, IndicadoresConsumo, TipoConteudo } from '../../shared/lib/types';
import styles from './MateriaisPage.module.css';

// Ícones de ação (favoritar/assistido/baixar) — SVG (ICON_PROPS), mesmo raciocínio de
// materialDisplay.tsx: emoji (★☆✓▶↓) destoava do traço linear usado no resto do app.
const ICONE_ESTRELA_CHEIA = (
  <svg {...ICON_PROPS} width={14} height={14} fill="currentColor" stroke="none">
    <path d="M12 3.5l2.6 5.6 6.1.6-4.6 4.1 1.3 6-5.4-3.1-5.4 3.1 1.3-6-4.6-4.1 6.1-.6L12 3.5Z" />
  </svg>
);
const ICONE_ESTRELA_VAZIA = (
  <svg {...ICON_PROPS} width={14} height={14}>
    <path d="M12 3.5l2.6 5.6 6.1.6-4.6 4.1 1.3 6-5.4-3.1-5.4 3.1 1.3-6-4.6-4.1 6.1-.6L12 3.5Z" />
  </svg>
);
const ICONE_CHECK_MATERIAL = (
  <svg {...ICON_PROPS} width={14} height={14}>
    <path d="M4 12l5 5L20 6" />
  </svg>
);
const ICONE_DOWNLOAD = (
  <svg {...ICON_PROPS} width={14} height={14}>
    <path d="M12 3v12M7 10l5 5 5-5" />
    <path d="M5 21h14" />
  </svg>
);
const ICONE_PLAY_PEQUENO = (
  <svg {...ICON_PROPS} width={14} height={14}>
    <circle cx="12" cy="12" r="9" />
    <path d="M10 8.5v7l6-3.5-6-3.5Z" fill="currentColor" stroke="none" />
  </svg>
);

const TABS: { label: string; view: 'CATALOGO' | 'DICAS' }[] = [
  { label: 'Biblioteca', view: 'CATALOGO' },
  { label: 'Dicas do Brayan', view: 'DICAS' },
];

const TIPOS: { label: string; valor: TipoConteudo | '' }[] = [
  { label: 'Todos os formatos', valor: '' },
  { label: 'Documentos', valor: 'DOCUMENTO' },
  { label: 'Planilhas', valor: 'PLANILHA' },
  { label: 'Apresentações', valor: 'APRESENTACAO' },
  { label: 'Vídeos', valor: 'VIDEO' },
];

export function MateriaisPage() {
  const [view, setView] = useState<'CATALOGO' | 'DICAS'>('CATALOGO');
  const [filtroTipo, setFiltroTipo] = useState<TipoConteudo | ''>('');
  const [mostrarFavoritos, setMostrarFavoritos] = useState(false);
  const [conteudos, setConteudos] = useState<Conteudo[] | null>(null);
  const [dicas, setDicas] = useState<Conteudo[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [processando, setProcessando] = useState<string | null>(null);
  const [indicadores, setIndicadores] = useState<IndicadoresConsumo | null>(null);

  const carregarIndicadores = () => {
    apiClient.get<IndicadoresConsumo>('/mentorado/conteudos/indicadores').then((res) => setIndicadores(res.data));
  };

  useEffect(carregarIndicadores, []);

  const carregarCatalogo = () => {
    setConteudos(null);
    apiClient
      .get<Conteudo[]>('/mentorado/conteudos', { 
        params: { 
          tipo: filtroTipo || undefined, 
          favorito: mostrarFavoritos ? true : undefined 
        } 
      })
      .then((res) => setConteudos(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar a biblioteca.')));
  };

  const carregarDicas = () => {
    if (dicas !== null) return; // Cache simples
    apiClient
      .get<Conteudo[]>('/mentorado/conteudos/dicas')
      .then((res) => setDicas(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar as dicas.')));
  };

  useEffect(() => {
    setError(null);
    if (view === 'CATALOGO') {
      carregarCatalogo();
    } else {
      carregarDicas();
    }
  }, [view, filtroTipo, mostrarFavoritos]);

  async function toggleStatus(id: string, campo: 'favorito' | 'assistido', valorAtual: boolean) {
    setProcessando(id);
    try {
      await apiClient.patch(`/mentorado/conteudos/${id}/${campo}`, { [campo]: !valorAtual });
      // Atualiza estado local sem recarregar tudo para ficar mais responsivo
      const updateFn = (list: Conteudo[] | null) => 
        list?.map(c => c.id === id ? { ...c, [campo]: !valorAtual } : c) || null;
      
      setConteudos(updateFn);
      setDicas(updateFn);
      carregarIndicadores();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível atualizar o item.'));
    } finally {
      setProcessando(null);
    }
  }

  return (
    <div className={styles.container}>
      <div>
        <h1 className={styles.title}>Materiais & Dicas</h1>
        <p className={styles.subtitle}>Acesse a base de conhecimento e acelere seus resultados.</p>
      </div>

      {indicadores && (
        <div className={styles.indicadores} data-testid="indicadores-consumo">
          <div className={styles.indicadorTile} data-testid="indicador-dias-assistidos">
            <div className={styles.indicadorValor}>{indicadores.diasAssistidos}</div>
            <div className={styles.indicadorLabel}>dias assistidos</div>
          </div>
          <div className={styles.indicadorTile} data-testid="indicador-favoritas">
            <div className={styles.indicadorValor}>{indicadores.favoritas}</div>
            <div className={styles.indicadorLabel}>favoritas</div>
          </div>
          <div className={styles.indicadorTile} data-testid="indicador-minutos">
            <div className={styles.indicadorValor}>{indicadores.minutosAssistidos}</div>
            <div className={styles.indicadorLabel}>minutos de conteúdo assistido</div>
          </div>
        </div>
      )}

      <div className={styles.toolbar}>
        <div className={styles.tabs}>
          {TABS.map((t) => (
            <button
              key={t.label}
              className={`${styles.tab} ${view === t.view ? styles.tabActive : ''}`}
              onClick={() => setView(t.view)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {view === 'CATALOGO' && (
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 14, color: 'var(--text-soft)', cursor: 'pointer' }}>
              <input 
                type="checkbox" 
                checked={mostrarFavoritos} 
                onChange={(e) => setMostrarFavoritos(e.target.checked)} 
              />
              Apenas favoritos
            </label>
            <select 
              value={filtroTipo} 
              onChange={(e) => setFiltroTipo(e.target.value as TipoConteudo | '')}
              style={{ padding: '6px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--surface)', color: 'var(--text)' }}
            >
              {TIPOS.map(t => <option key={t.label} value={t.valor}>{t.label}</option>)}
            </select>
          </div>
        )}
      </div>

      {error && <div style={{ color: 'var(--danger)', padding: 12, background: 'var(--danger-bg)', borderRadius: 6 }}>{error}</div>}

      {view === 'DICAS' && (
        <div className={styles.dicasGrid}>
          {dicas === null && !error && <div className={styles.emptyState}>Carregando dicas...</div>}
          {dicas?.length === 0 && <div className={styles.emptyState}>Nenhuma dica encontrada.</div>}
          {dicas?.map((dica) => (
            <Card key={dica.id} className={styles.dicaCard} testId={`dica-${dica.id}`}>
              <div 
                className={styles.videoPlaceholder} 
                onClick={() => {
                  // noopener,noreferrer — mesmo motivo do rel="noreferrer" já usado nos <a> desta
                  // página (M11, achado do revisor-seguranca): sem isso, a aba aberta mantém
                  // acesso a window.opener (reverse tabnabbing).
                  window.open(dica.url, '_blank', 'noopener,noreferrer');
                  if (!dica.assistido) toggleStatus(dica.id, 'assistido', false);
                }}
              >
                <div className={styles.videoPlay}>
                  <div className={styles.dicaPlayIcon}></div>
                </div>
              </div>
              <div className={styles.dicaContent}>
                <h3 className={styles.dicaTitle}>{dica.titulo}</h3>
                <div className={styles.dicaFooter}>
                  <span className={styles.dicaDate}>{new Date(dica.criadoEm).toLocaleDateString('pt-BR')}</span>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button
                      className={`${styles.actionButton} ${dica.assistido ? styles.assistido : ''}`}
                      disabled={processando === dica.id}
                      onClick={() => toggleStatus(dica.id, 'assistido', !!dica.assistido)}
                      title={dica.assistido ? "Marcar como não assistido" : "Marcar como assistido"}
                      aria-pressed={!!dica.assistido}
                      data-testid={`assistido-dica-${dica.id}`}
                    >
                      {dica.assistido ? <>{ICONE_CHECK_MATERIAL} Assistido</> : 'Marcar assistido'}
                    </button>
                    <button
                      className={`${styles.actionButton} ${dica.favorito ? styles.favorito : ''}`}
                      disabled={processando === dica.id}
                      onClick={() => toggleStatus(dica.id, 'favorito', !!dica.favorito)}
                      title={dica.favorito ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
                      aria-label={dica.favorito ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
                      aria-pressed={!!dica.favorito}
                      data-testid={`favoritar-dica-${dica.id}`}
                    >
                      {dica.favorito ? ICONE_ESTRELA_CHEIA : ICONE_ESTRELA_VAZIA}
                    </button>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {view === 'CATALOGO' && (
        <div className={styles.materiaisList}>
          {conteudos === null && !error && <div className={styles.emptyState}>Carregando biblioteca...</div>}
          {conteudos?.length === 0 && <div className={styles.emptyState}>Nenhum material encontrado com os filtros atuais.</div>}
          {conteudos?.map((c) => (
            <Card key={c.id} className={styles.materialCard} testId={`material-${c.id}`}>
              <div className={styles.materialInfo}>
                <div className={styles.materialIcon}>{TIPO_ICONE[c.tipo]}</div>
                <div className={styles.materialDetails}>
                  <a href={c.url} target="_blank" rel="noreferrer" className={styles.materialTitle}>
                    {c.titulo}
                  </a>
                  <div className={styles.materialMeta}>
                    <span>{new Date(c.criadoEm).toLocaleDateString('pt-BR')}</span>
                    <span>•</span>
                    <span>{c.tipo.charAt(0) + c.tipo.slice(1).toLowerCase()}</span>
                  </div>
                </div>
              </div>
              <div className={styles.materialActions}>
                <button
                  className={`${styles.actionButton} ${c.favorito ? styles.favorito : ''}`}
                  disabled={processando === c.id}
                  onClick={() => toggleStatus(c.id, 'favorito', !!c.favorito)}
                  title={c.favorito ? "Remover dos favoritos" : "Adicionar aos favoritos"}
                  aria-pressed={!!c.favorito}
                  data-testid={`favoritar-material-${c.id}`}
                >
                  {c.favorito ? <>{ICONE_ESTRELA_CHEIA} Favorito</> : <>{ICONE_ESTRELA_VAZIA} Favoritar</>}
                </button>
                <a href={c.url} target="_blank" rel="noreferrer" className={styles.downloadLink}>
                  Acessar {c.tipo === 'VIDEO' ? ICONE_PLAY_PEQUENO : ICONE_DOWNLOAD}
                </a>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
