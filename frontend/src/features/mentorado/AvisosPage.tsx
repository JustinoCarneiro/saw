import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { CATEGORIA_COR, CATEGORIA_ICONE, CATEGORIA_LABEL } from '../../shared/lib/avisoDisplay';
import { formatarQuando } from '../../shared/lib/format';
import type { AvisoMentorado, CategoriaAviso } from '../../shared/lib/types';
import styles from './AvisosPage.module.css';

const FILTROS: { label: string; categoria: CategoriaAviso | ''; apenasNaoLidos?: boolean }[] = [
  { label: 'Todos', categoria: '' },
  { label: 'Não lidos', categoria: '', apenasNaoLidos: true },
  { label: 'Mentorias', categoria: 'MENTORIAS' },
  { label: 'Materiais', categoria: 'MATERIAIS' },
  { label: 'Eventos', categoria: 'EVENTOS' },
];

export function AvisosPage() {
  const [filtroIndex, setFiltroIndex] = useState(0);
  const [avisos, setAvisos] = useState<AvisoMentorado[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [marcandoTodos, setMarcandoTodos] = useState(false);

  const filtro = FILTROS[filtroIndex];

  const carregar = () => {
    setAvisos(null);
    apiClient
      .get<AvisoMentorado[]>('/mentorado/avisos', {
        params: { categoria: filtro.categoria || undefined, apenasNaoLidos: filtro.apenasNaoLidos },
      })
      .then((res) => setAvisos(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar seus avisos.')));
  };

  useEffect(carregar, [filtroIndex]);

  async function marcarLido(id: string) {
    try {
      await apiClient.patch(`/mentorado/avisos/${id}/lido`);
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível marcar o aviso como lido.'));
    }
  }

  async function marcarTodosLidos() {
    setMarcandoTodos(true);
    try {
      await apiClient.patch('/mentorado/avisos/marcar-todos-lidos');
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível marcar todos como lidos.'));
    } finally {
      setMarcandoTodos(false);
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.headerRow}>
        <div>
          <h1 className={styles.title}>Avisos</h1>
          <p className={styles.subtitle}>Fique por dentro de tudo que acontece na SAW.</p>
        </div>
        <button className={styles.marcarTodosButton} onClick={marcarTodosLidos} disabled={marcandoTodos} data-testid="marcar-todos-lidos">
          Marcar todos como lidos
        </button>
      </div>

      <div className={styles.tabs}>
        {FILTROS.map((f, i) => (
          <button
            key={f.label}
            className={`${styles.tab} ${filtroIndex === i ? styles.tabActive : ''}`}
            onClick={() => setFiltroIndex(i)}
            data-testid={`avisos-filtro-${f.label.toLowerCase().replace(/\s+/g, '-')}`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <div className={styles.error} data-testid="avisos-erro">{error}</div>}

      {avisos === null && !error && <div className={styles.emptyState}>Carregando…</div>}
      {avisos?.length === 0 && <div className={styles.emptyState}>Nenhum aviso encontrado.</div>}

      <div className={styles.lista}>
        {avisos?.map((a) => (
          <Card
            key={a.id}
            testId={`aviso-${a.id}`}
            className={a.lido ? undefined : styles.cardNaoLida}
            style={{ padding: '16px 18px', cursor: a.lido ? 'default' : 'pointer' }}
          >
            <div className={styles.linha} onClick={() => !a.lido && marcarLido(a.id)}>
              <span className={styles.icone} style={{ background: CATEGORIA_COR[a.categoria].bg }}>
                {CATEGORIA_ICONE[a.categoria]}
              </span>
              <div className={styles.conteudo}>
                <div className={styles.tituloLinha}>
                  <span className={styles.avisoTitulo}>{a.titulo}</span>
                  <Pill bg={CATEGORIA_COR[a.categoria].bg} color={CATEGORIA_COR[a.categoria].color}>
                    {CATEGORIA_LABEL[a.categoria]}
                  </Pill>
                </div>
                <div className={styles.desc}>{a.descricao}</div>
                <div className={styles.quando}>{formatarQuando(a.quando)}</div>
              </div>
              {!a.lido && <span className={styles.dot} data-testid={`aviso-nao-lido-${a.id}`} />}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
