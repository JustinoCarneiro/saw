import { useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Card } from '../../shared/components/Card';
import { ICON_PROPS } from '../../shared/components/iconProps';
import { Pill } from '../../shared/components/Pill';
import { Tooltip } from '../../shared/components/Tooltip';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { MentoriaMentorado } from '../../shared/lib/types';
import styles from './MentoriasPage.module.css';

// Achado de UX: '▲'/'▼' Unicode destoava do traço linear (ICON_PROPS) usado no resto do app —
// um único chevron pra baixo que gira via CSS (.expandIcon.aberta) em vez de trocar de glifo.
const ICONE_CHEVRON = (
  <svg {...ICON_PROPS} width={14} height={14}>
    <path d="M6 9l6 6 6-6" />
  </svg>
);

function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

const TIPO_LABEL: Record<MentoriaMentorado['tipo'], string> = {
  INDIVIDUAL: 'Mentoria individual',
  GRUPO: 'Mentoria em grupo',
};

function statusPill(status: MentoriaMentorado['status']): { label: string; bg: string; color: string } {
  switch (status) {
    case 'AGENDADA': return { label: 'Agendada', bg: 'var(--line)', color: 'var(--text-soft)' };
    case 'CONFIRMADA': return { label: 'Confirmada', bg: 'var(--info-bg)', color: 'var(--info)' };
    case 'REALIZADA': return { label: 'Realizada', bg: 'var(--success-bg)', color: 'var(--success)' };
    case 'CANCELADA': return { label: 'Cancelada', bg: 'var(--danger-bg)', color: 'var(--danger)' };
  }
}

// H5.3 — mesmo formato UTC compacto do .ics gerado no backend (IcsGenerator), pra montar a URL de
// "render" do Google Calendar 100% no frontend, sem round-trip extra (ver ROADMAP.md M12).
function paraGoogleCalendarUtc(iso: string): string {
  return iso.replace(/\.\d+/, '').replace(/[-:]/g, '');
}

function googleCalendarUrl(m: MentoriaMentorado): string {
  const fim = new Date(new Date(m.dataHora).getTime() + m.duracaoMin * 60000).toISOString();
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: `Mentoria SAW HUB — ${m.mentorNome}`,
    dates: `${paraGoogleCalendarUtc(m.dataHora)}/${paraGoogleCalendarUtc(fim)}`,
    location: m.linkOnline ?? m.local ?? '',
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

export function MentoriasPage() {
  const [mentorias, setMentorias] = useState<MentoriaMentorado[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [expandida, setExpandida] = useState<string | null>(null);
  const [baixando, setBaixando] = useState<string | null>(null);

  useEffect(() => {
    apiClient.get<MentoriaMentorado[]>('/mentorado/mentorias')
      .then((res) => setMentorias(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar suas mentorias.')));
  }, []);

  async function baixarIcs(m: MentoriaMentorado) {
    setBaixando(m.id);
    try {
      const res = await apiClient.get(`/mentorado/mentorias/${m.id}/calendario.ics`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data as Blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'mentoria.ics';
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível gerar o arquivo de calendário.'));
    } finally {
      setBaixando(null);
    }
  }

  const agenda = (mentorias ?? [])
    .filter((m) => m.status === 'AGENDADA' || m.status === 'CONFIRMADA')
    .sort((a, b) => a.dataHora.localeCompare(b.dataHora));
  const historico = (mentorias ?? [])
    .filter((m) => m.status === 'REALIZADA' || m.status === 'CANCELADA')
    .sort((a, b) => b.dataHora.localeCompare(a.dataHora));

  return (
    <div className={styles.container}>
      <div>
        <h1 className={styles.title}>Mentorias & Atas</h1>
        <p className={styles.subtitle}>Sua agenda de mentorias, atas e materiais recomendados.</p>
      </div>

      {error && <div className={styles.error}>{error}</div>}
      {mentorias === null && !error && <div className={styles.emptyState}>Carregando…</div>}

      {mentorias !== null && (
        <>
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>
              <Tooltip text="Suas mentorias com status Agendada ou Confirmada, ainda não realizadas.">Agenda</Tooltip>
            </h2>
            {agenda.length === 0 && <div className={styles.emptyState}>Nenhuma mentoria agendada.</div>}
            <div className={styles.agendaGrid}>
              {agenda.map((m) => {
                const pill = statusPill(m.status);
                return (
                  <Card key={m.id} testId={`mentoria-agenda-${m.id}`} style={{ padding: 20 }}>
                    <div className={styles.cardHeader}>
                      <span className={styles.tipo}>{TIPO_LABEL[m.tipo]}</span>
                      <Pill bg={pill.bg} color={pill.color}>{pill.label}</Pill>
                    </div>
                    <div className={styles.dataHora}>{formatarDataHora(m.dataHora)}</div>
                    <div className={styles.mentorLine}>com {m.mentorNome}</div>
                    {m.local && <div className={styles.localLine}>{m.local}</div>}
                    <div className={styles.cardActions}>
                      {m.linkOnline && (m.podeEntrarAgora ? (
                        <a
                          className={styles.joinButton}
                          href={m.linkOnline}
                          target="_blank"
                          rel="noreferrer"
                          data-testid={`entrar-reuniao-${m.id}`}
                        >
                          Entrar na reunião
                        </a>
                      ) : (
                        <button
                          className={styles.joinButtonDisabled}
                          disabled
                          title="Disponível 10 minutos antes do início"
                          data-testid={`entrar-reuniao-${m.id}`}
                        >
                          Entrar na reunião
                        </button>
                      ))}
                      <button className={styles.actionButton} disabled={baixando === m.id} onClick={() => baixarIcs(m)}>
                        .ics
                      </button>
                      <a className={styles.actionButton} href={googleCalendarUrl(m)} target="_blank" rel="noreferrer">
                        Google Calendar
                      </a>
                    </div>
                  </Card>
                );
              })}
            </div>
          </section>

          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>
              <Tooltip text="Mentorias já Realizadas. Abra pra ver a ata publicada de cada uma.">Histórico</Tooltip>
            </h2>
            {historico.length === 0 && <div className={styles.emptyState}>Nenhuma mentoria realizada ainda.</div>}
            <div className={styles.historicoList}>
              {historico.map((m) => {
                const pill = statusPill(m.status);
                const aberta = expandida === m.id;
                return (
                  <Card key={m.id} testId={`mentoria-historico-${m.id}`} style={{ padding: 16 }}>
                    <button
                      className={styles.historicoHeader}
                      onClick={() => setExpandida(aberta ? null : m.id)}
                      disabled={m.status !== 'REALIZADA'}
                      data-testid={`expandir-ata-${m.id}`}
                    >
                      <div>
                        <div className={styles.tipo}>{TIPO_LABEL[m.tipo]} — {m.mentorNome}</div>
                        <div className={styles.historicoData}>{formatarDataHora(m.dataHora)}</div>
                      </div>
                      <div className={styles.historicoRight}>
                        <Pill bg={pill.bg} color={pill.color}>{pill.label}</Pill>
                        {m.status === 'REALIZADA' && (
                          <span className={`${styles.expandIcon} ${aberta ? styles.expandIconAberta : ''}`}>{ICONE_CHEVRON}</span>
                        )}
                      </div>
                    </button>
                    {aberta && m.status === 'REALIZADA' && (
                      <div className={styles.ataBody} data-testid={`ata-${m.id}`}>
                        {m.ata ? (
                          <>
                            <p className={styles.ataResumo}>{m.ata.resumo}</p>
                            <div className={styles.ataPublicada}>Publicada em {formatarDataHora(m.ata.publicadaEm)}</div>
                          </>
                        ) : (
                          <p className={styles.ataPendente}>A ata desta mentoria ainda não foi publicada.</p>
                        )}
                        {m.materiaisRecomendados.length > 0 && (
                          <div className={styles.materiais}>
                            <span className={styles.materiaisLabel}>Materiais recomendados:</span>
                            {m.materiaisRecomendados.map((mat) => (
                              <a key={mat.id} href={mat.url} target="_blank" rel="noreferrer" className={styles.materialChip}>
                                {mat.titulo}
                              </a>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </Card>
                );
              })}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
