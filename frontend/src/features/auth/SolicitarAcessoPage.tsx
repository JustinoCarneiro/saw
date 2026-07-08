import { type FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import type { Plano } from '../../shared/lib/types';
import styles from './SolicitarAcessoPage.module.css';

const PLANO_LABEL: Record<Plano, string> = {
  GRATUITO: 'Gratuito',
  BASICO: 'Básico',
  ESSENCIAL: 'Essencial',
  PROFISSIONAL: 'Profissional',
};

export function SolicitarAcessoPage() {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [telefone, setTelefone] = useState('');
  const [mensagem, setMensagem] = useState('');
  const [planoInteresse, setPlanoInteresse] = useState<Plano | ''>('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [enviado, setEnviado] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/leads', {
        nome,
        email,
        telefone: telefone || null,
        mensagem: mensagem || null,
        planoInteresse: planoInteresse || null,
      });
      setEnviado(true);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível enviar sua solicitação. Tente novamente.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.logoBlock}>
          <svg width="44" height="44" viewBox="0 0 44 44" fill="none">
            <rect x="22" y="4" width="25.5" height="25.5" rx="4" transform="rotate(45 22 4)" stroke="#F0B050" strokeWidth="1.6" />
            <rect x="22" y="13" width="12.7" height="12.7" rx="2" transform="rotate(45 22 13)" stroke="#F0B050" strokeWidth="1.4" />
          </svg>
          <div className={styles.wordmark}>SAW</div>
          <div className={styles.eyebrow}>ESCOLA DE RESTAURANTES</div>
        </div>

        {enviado ? (
          <div className={styles.confirmacao}>
            <h1 className={styles.headline}>Solicitação enviada.</h1>
            <p className={styles.confirmacaoTexto}>
              Recebemos seu pedido de acesso. Nosso time comercial vai entrar em contato em breve.
            </p>
            <Link className={styles.submit} to="/login">
              Voltar para o login
            </Link>
          </div>
        ) : (
          <form className={styles.form} onSubmit={handleSubmit}>
            <h1 className={styles.headline}>Solicitar acesso</h1>
            <p className={styles.subtitle}>Conte um pouco sobre você — entramos em contato em breve.</p>

            <label className={styles.label} htmlFor="nome">Nome</label>
            <input
              id="nome"
              className={styles.input}
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              maxLength={120}
              required
            />

            <label className={styles.label} htmlFor="email">E-mail</label>
            <input
              id="email"
              className={styles.input}
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              maxLength={255}
              required
            />

            <label className={styles.label} htmlFor="telefone">Telefone (opcional)</label>
            <input
              id="telefone"
              className={styles.input}
              value={telefone}
              onChange={(e) => setTelefone(e.target.value)}
              maxLength={20}
            />

            <label className={styles.label} htmlFor="plano">Plano de interesse (opcional)</label>
            <select
              id="plano"
              className={styles.input}
              value={planoInteresse}
              onChange={(e) => setPlanoInteresse(e.target.value as Plano | '')}
            >
              <option value="">Não sei ainda</option>
              {(Object.keys(PLANO_LABEL) as Plano[]).map((p) => (
                <option key={p} value={p}>{PLANO_LABEL[p]}</option>
              ))}
            </select>

            <label className={styles.label} htmlFor="mensagem">Mensagem (opcional)</label>
            <textarea
              id="mensagem"
              className={styles.textarea}
              value={mensagem}
              onChange={(e) => setMensagem(e.target.value)}
              maxLength={500}
              rows={3}
            />

            {error && <div className={styles.error}>{error}</div>}

            <button className={styles.submit} type="submit" disabled={submitting}>
              {submitting ? 'Enviando…' : 'Enviar solicitação'}
            </button>
            <Link className={styles.outlineButton} to="/login">
              Voltar para o login
            </Link>
          </form>
        )}
      </div>
    </div>
  );
}
