import { useEffect, useMemo, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { Pill } from '../../shared/components/Pill';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { EventoMentorado, TipoEvento } from '../../shared/lib/types';
import styles from './EventosMentoradoPage.module.css';

function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

const TIPO_LABEL: Record<TipoEvento, string> = {
  AO_VIVO: 'Ao vivo',
  PRESENCIAL: 'Presencial',
};

function statusPill(status: EventoMentorado['status']): { label: string; bg: string; color: string } {
  return status === 'AO_VIVO'
    ? { label: 'Ao vivo agora', bg: 'var(--danger-bg)', color: 'var(--danger)' }
    : { label: 'Programado', bg: 'var(--info-bg)', color: 'var(--info)' };
}

function mesmoDia(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

const DIAS_SEMANA = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'];
const NOME_MES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];

function celulasDoMes(mesAtual: Date): (Date | null)[] {
  const primeiroDia = new Date(mesAtual.getFullYear(), mesAtual.getMonth(), 1);
  const ultimoDia = new Date(mesAtual.getFullYear(), mesAtual.getMonth() + 1, 0);
  const celulas: (Date | null)[] = new Array(primeiroDia.getDay()).fill(null);
  for (let dia = 1; dia <= ultimoDia.getDate(); dia++) {
    celulas.push(new Date(mesAtual.getFullYear(), mesAtual.getMonth(), dia));
  }
  return celulas;
}

export function EventosMentoradoPage() {
  const [eventos, setEventos] = useState<EventoMentorado[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filtroTipo, setFiltroTipo] = useState<TipoEvento | ''>('');
  const [busca, setBusca] = useState('');
  const [processando, setProcessando] = useState<string | null>(null);
  const [mesAtual, setMesAtual] = useState(() => new Date());
  const [diaSelecionado, setDiaSelecionado] = useState<Date | null>(null);

  const carregar = () => {
    apiClient.get<EventoMentorado[]>('/mentorado/eventos', { params: { tipo: filtroTipo || undefined, tema: busca || undefined } })
      .then((res) => setEventos(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar os eventos.')));
  };

  useEffect(carregar, [filtroTipo, busca]);

  async function inscrever(id: string) {
    setProcessando(id);
    try {
      await apiClient.post(`/mentorado/eventos/${id}/inscricao`);
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível concluir a inscrição.'));
    } finally {
      setProcessando(null);
    }
  }

  async function cancelar(id: string) {
    setProcessando(id);
    try {
      await apiClient.delete(`/mentorado/eventos/${id}/inscricao`);
      carregar();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível cancelar a inscrição.'));
    } finally {
      setProcessando(null);
    }
  }

  const contagemPorDia = useMemo(() => {
    const mapa = new Map<string, number>();
    (eventos ?? []).forEach((e) => {
      const data = new Date(e.dataHora);
      const chave = `${data.getFullYear()}-${data.getMonth()}-${data.getDate()}`;
      mapa.set(chave, (mapa.get(chave) ?? 0) + 1);
    });
    return mapa;
  }, [eventos]);

  const listaExibida = (eventos ?? []).filter((e) => !diaSelecionado || mesmoDia(new Date(e.dataHora), diaSelecionado));
  const meusEventos = listaExibida.filter((e) => e.inscrito);
  const disponiveis = listaExibida.filter((e) => !e.inscrito);

  function renderCard(e: EventoMentorado) {
    const pill = statusPill(e.status);
    const semVaga = e.vagasDisponiveis !== null && e.vagasDisponiveis <= 0;
    return (
      <Card key={e.id} testId={`evento-${e.id}`} style={{ padding: 20 }}>
        <div className={styles.cardHeader}>
          <span className={styles.tipo}>{TIPO_LABEL[e.tipo]}{e.tema ? ` · ${e.tema}` : ''}</span>
          <Pill bg={pill.bg} color={pill.color}>{pill.label}</Pill>
        </div>
        <div className={styles.titulo}>{e.titulo}</div>
        <div className={styles.dataHora}>{formatarDataHora(e.dataHora)}</div>
        {e.local && <div className={styles.localLine}>{e.local}</div>}
        <div className={styles.vagasLine}>
          {e.vagasDisponiveis === null ? 'Sem limite de vagas' : `${e.vagasDisponiveis} de ${e.vagas} vaga(s) disponível(is)`}
        </div>
        <div className={styles.cardActions}>
          {e.inscrito ? (
            <button className={styles.cancelButton} disabled={processando === e.id} onClick={() => cancelar(e.id)} data-testid={`cancelar-${e.id}`}>
              ✓ Inscrito — cancelar
            </button>
          ) : (
            <button
              className={styles.subscribeButton}
              disabled={processando === e.id || semVaga}
              onClick={() => inscrever(e.id)}
              data-testid={`inscrever-${e.id}`}
              title={semVaga ? 'Sem vagas disponíveis' : undefined}
            >
              {semVaga ? 'Sem vagas' : 'Inscrever-se'}
            </button>
          )}
          {e.linkOnline && (
            <a className={styles.actionButton} href={e.linkOnline} target="_blank" rel="noreferrer">Link do evento</a>
          )}
        </div>
      </Card>
    );
  }

  return (
    <div className={styles.container}>
      <div>
        <h1 className={styles.title}>Eventos</h1>
        <p className={styles.subtitle}>Eventos ao vivo e presenciais da SAW — inscreva-se e acompanhe sua agenda.</p>
      </div>

      <div className={styles.toolbar}>
        <select value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value as TipoEvento | '')} className={styles.select}>
          <option value="">Todos os tipos</option>
          <option value="AO_VIVO">Ao vivo</option>
          <option value="PRESENCIAL">Presencial</option>
        </select>
        <input
          className={styles.searchInput}
          placeholder="Buscar por tema..."
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
        />
      </div>

      {error && <div className={styles.error}>{error}</div>}

      <Card style={{ padding: 16 }} testId="calendario-eventos">
        <div className={styles.calendarHeader}>
          <button className={styles.navButton} onClick={() => setMesAtual(new Date(mesAtual.getFullYear(), mesAtual.getMonth() - 1, 1))}>‹</button>
          <span className={styles.mesLabel}>{NOME_MES[mesAtual.getMonth()]} {mesAtual.getFullYear()}</span>
          <button className={styles.navButton} onClick={() => setMesAtual(new Date(mesAtual.getFullYear(), mesAtual.getMonth() + 1, 1))}>›</button>
          {diaSelecionado && (
            <button className={styles.clearButton} onClick={() => setDiaSelecionado(null)}>Limpar seleção</button>
          )}
        </div>
        <div className={styles.calendarGrid}>
          {DIAS_SEMANA.map((d, i) => <div key={i} className={styles.weekdayLabel}>{d}</div>)}
          {celulasDoMes(mesAtual).map((dia, i) => {
            if (!dia) return <div key={i} className={styles.emptyCell} />;
            const chave = `${dia.getFullYear()}-${dia.getMonth()}-${dia.getDate()}`;
            const qtd = contagemPorDia.get(chave) ?? 0;
            const selecionado = diaSelecionado && mesmoDia(dia, diaSelecionado);
            return (
              <button
                key={i}
                className={`${styles.dayCell} ${qtd > 0 ? styles.dayCellWithEvent : ''} ${selecionado ? styles.dayCellSelected : ''}`}
                onClick={() => setDiaSelecionado(selecionado ? null : dia)}
                disabled={qtd === 0}
                data-testid={`dia-${dia.getFullYear()}-${String(dia.getMonth() + 1).padStart(2, '0')}-${String(dia.getDate()).padStart(2, '0')}`}
              >
                {dia.getDate()}
                {qtd > 0 && <span className={styles.dayDot} />}
              </button>
            );
          })}
        </div>
      </Card>

      {eventos === null && !error && <div className={styles.emptyState}>Carregando eventos…</div>}

      {eventos !== null && (
        <>
          {meusEventos.length > 0 && (
            <section className={styles.section}>
              <h2 className={styles.sectionTitle}>Próximos eventos (você está inscrito)</h2>
              <div className={styles.grid}>{meusEventos.map(renderCard)}</div>
            </section>
          )}
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>{diaSelecionado ? 'Eventos neste dia' : 'Eventos disponíveis'}</h2>
            {disponiveis.length === 0 && <div className={styles.emptyState}>Nenhum evento encontrado.</div>}
            <div className={styles.grid}>{disponiveis.map(renderCard)}</div>
          </section>
        </>
      )}
    </div>
  );
}
