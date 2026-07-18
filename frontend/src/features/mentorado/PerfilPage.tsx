import { type FormEvent, useEffect, useState } from 'react';
import { apiClient } from '../../shared/lib/apiClient';
import { Avatar } from '../../shared/components/Avatar';
import { Card } from '../../shared/components/Card';
import { Pill } from '../../shared/components/Pill';
import { ProgressBar } from '../../shared/components/ProgressBar';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Assinatura, Jornada, PerfilMentorado, Plano } from '../../shared/lib/types';
import styles from './PerfilPage.module.css';

const NIVEL_LABEL: Record<string, string> = { BRONZE: 'Bronze', PRATA: 'Prata', OURO: 'Ouro', DIAMANTE: 'Diamante' };
const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito', BASICO: 'Básico', ESSENCIAL: 'Essencial', PROFISSIONAL: 'Profissional',
};

function formatarData(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso.length === 10 ? iso + 'T00:00:00' : iso).toLocaleDateString('pt-BR');
}

export function PerfilPage() {
  const [perfil, setPerfil] = useState<PerfilMentorado | null>(null);
  const [jornada, setJornada] = useState<Jornada | null>(null);
  const [assinatura, setAssinatura] = useState<Assinatura | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState(false);

  const carregar = () => {
    apiClient.get<PerfilMentorado>('/mentorado/perfil')
      .then((res) => setPerfil(res.data))
      .catch((err) => setError(getApiErrorMessage(err, 'Não foi possível carregar seu perfil.')));
    apiClient.get<Jornada>('/mentorado/perfil/jornada').then((res) => setJornada(res.data));
    apiClient.get<Assinatura>('/mentorado/perfil/assinatura').then((res) => setAssinatura(res.data));
  };

  useEffect(carregar, []);

  return (
    <div className={styles.container}>
      <div className={styles.headerRow}>
        <div>
          <h1 className={styles.title}>Meu perfil</h1>
          <p className={styles.subtitle}>Gerencie suas informações, preferências e assinatura.</p>
        </div>
        {!editando && (
          <button className={styles.editButton} onClick={() => setEditando(true)} data-testid="editar-perfil">
            Editar perfil
          </button>
        )}
      </div>

      {error && <div className={styles.error} data-testid="perfil-erro">{error}</div>}

      <div className={styles.layout}>
        <div className={styles.main}>
          {perfil === null && !error && <div className={styles.emptyState}>Carregando…</div>}

          {perfil && !editando && (
            <Card style={{ padding: 24 }} testId="perfil-cartao">
              <div className={styles.identidade}>
                <Avatar name={perfil.nome} size={56} />
                <div>
                  <div className={styles.nome}>{perfil.nome}</div>
                  <div className={styles.negocio}>{perfil.negocio ?? '—'}</div>
                </div>
              </div>
              <dl className={styles.contatoGrid}>
                <div><dt>E-mail</dt><dd>{perfil.email}</dd></div>
                <div><dt>Telefone</dt><dd>{perfil.telefone ?? '—'}</dd></div>
                <div><dt>Membro desde</dt><dd>{formatarData(perfil.membroDesde)}</dd></div>
              </dl>
              {perfil.bio && <p className={styles.bio}>{perfil.bio}</p>}
            </Card>
          )}

          {perfil && editando && (
            <PerfilForm
              perfil={perfil}
              onSalvo={(atualizado) => { setPerfil(atualizado); setEditando(false); }}
              onCancelar={() => setEditando(false)}
            />
          )}

          {jornada && (
            <Card style={{ padding: 24, marginTop: 20 }} testId="jornada-cartao">
              <div className={styles.jornadaHeader}>
                <div>
                  <div className={styles.jornadaTitulo}>Minha jornada SAW</div>
                  <div className={styles.nivelLabel}>
                    Nível atual: <strong>{NIVEL_LABEL[jornada.nivelAtual]}</strong>
                  </div>
                </div>
                <div className={styles.xpLabel}>
                  {jornada.xp} {jornada.xpProximoNivel ? `/ ${jornada.xpProximoNivel} XP` : 'XP (nível máximo)'}
                </div>
              </div>
              <ProgressBar pct={jornada.progressoPct} />

              <div className={styles.statsGrid}>
                <StatTile label="Materiais acessados" valor={jornada.stats.materiaisAcessados} />
                <StatTile label="Dicas assistidas" valor={jornada.stats.dicasAssistidas} />
                <StatTile label="Eventos participados" valor={jornada.stats.eventosParticipados} />
                <StatTile label="Mentorias realizadas" valor={jornada.stats.mentoriasRealizadas} />
              </div>

              <div className={styles.conquistasTitulo}>Conquistas</div>
              <div className={styles.conquistasGrid}>
                {jornada.conquistas.map((c) => (
                  <div
                    key={c.codigo}
                    className={`${styles.conquista} ${c.desbloqueada ? styles.conquistaDesbloqueada : styles.conquistaBloqueada}`}
                    data-testid={`conquista-${c.codigo}`}
                  >
                    <div className={styles.conquistaTitulo}>{c.titulo}</div>
                    <div className={styles.conquistaDesc}>{c.descricao}</div>
                    {c.desbloqueada && (
                      <div className={styles.conquistaData}>
                        {c.desbloqueadaEm ? formatarData(c.desbloqueadaEm) : 'Desde sempre'}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>

        <div className={styles.sidebar}>
          {assinatura && (
            <Card style={{ padding: 20 }} testId="assinatura-cartao">
              <div className={styles.sidebarTitulo}>Minha assinatura</div>
              <div className={styles.assinaturaLinha}>
                <span>Plano atual</span>
                <Pill bg="var(--gold)" color="var(--on-gold)">{PLANO_LABEL[assinatura.planoAtual]}</Pill>
              </div>
              <div className={styles.assinaturaLinha}>
                <span>Vencimento</span>
                <span>{formatarData(assinatura.vencimentoPlano)}</span>
              </div>
              {assinatura.planosDisponiveis.some((p) => p.acimaDoPlanoAtual) && (
                <>
                  <div className={styles.sidebarSubtitulo}>Opções de upgrade</div>
                  <ul className={styles.planosLista}>
                    {assinatura.planosDisponiveis.filter((p) => p.acimaDoPlanoAtual).map((p) => (
                      <li key={p.plano}>{PLANO_LABEL[p.plano]}</li>
                    ))}
                  </ul>
                  <p className={styles.planosNota}>Fale com seu mentor pra solicitar a troca de plano.</p>
                </>
              )}
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}

function StatTile({ label, valor }: { label: string; valor: number }) {
  return (
    <div className={styles.statTile}>
      <div className={styles.statValor}>{valor}</div>
      <div className={styles.statLabel}>{label}</div>
    </div>
  );
}

function PerfilForm({ perfil, onSalvo, onCancelar }: {
  perfil: PerfilMentorado;
  onSalvo: (perfil: PerfilMentorado) => void;
  onCancelar: () => void;
}) {
  const [telefone, setTelefone] = useState(perfil.telefone ?? '');
  const [bio, setBio] = useState(perfil.bio ?? '');
  const [fotoUrl, setFotoUrl] = useState(perfil.fotoUrl ?? '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const res = await apiClient.patch<PerfilMentorado>('/mentorado/perfil', {
        telefone: telefone || null,
        bio: bio || null,
        fotoUrl: fotoUrl || null,
      });
      onSalvo(res.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível salvar seu perfil. Confira os dados.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card style={{ padding: 24 }} testId="perfil-form">
      <div className={styles.formTitle}>Editar perfil</div>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.formField}>
          Telefone
          <input className={styles.textInput} value={telefone} onChange={(e) => setTelefone(e.target.value)} placeholder="(11) 90000-0000" />
        </label>
        <label className={styles.formField}>
          Sobre mim
          <textarea className={styles.textarea} value={bio} onChange={(e) => setBio(e.target.value)} />
        </label>
        <label className={styles.formField}>
          Foto de perfil (URL)
          <input className={styles.textInput} value={fotoUrl} onChange={(e) => setFotoUrl(e.target.value)} placeholder="https://..." />
        </label>
        {error && <div className={styles.error}>{error}</div>}
        <div className={styles.formActions}>
          <button type="button" className={styles.cancelButton} onClick={onCancelar}>Cancelar</button>
          <button type="submit" className={styles.editButton} disabled={submitting}>
            {submitting ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </Card>
  );
}
