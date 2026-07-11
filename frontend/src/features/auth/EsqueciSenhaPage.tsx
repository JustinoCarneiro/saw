import { type FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import logoSaw from '../../assets/logo-saw.png';
import { apiClient } from '../../shared/lib/apiClient';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import styles from './SolicitarAcessoPage.module.css';

export function EsqueciSenhaPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [enviado, setEnviado] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      // Resposta sempre a mesma independente de o e-mail existir (H1.4) — o front só reflete
      // o que o backend já garante, não decide isso aqui.
      await apiClient.post('/auth/esqueci-senha', { email });
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
          <img className={styles.logoImg} src={logoSaw} alt="SAW — Escola de Restaurantes" />
        </div>

        {enviado ? (
          <div className={styles.confirmacao} data-testid="esqueci-senha-confirmacao">
            <h1 className={styles.headline}>Verifique seu e-mail.</h1>
            <p className={styles.confirmacaoTexto}>
              Se esse e-mail existir na nossa base, você vai receber um link de redefinição em
              instantes. O link é válido por 30 minutos.
            </p>
            <Link className={styles.submit} to="/login">
              Voltar para o login
            </Link>
          </div>
        ) : (
          <form className={styles.form} onSubmit={handleSubmit}>
            <h1 className={styles.headline}>Esqueci minha senha</h1>
            <p className={styles.subtitle}>Informe seu e-mail e enviamos um link pra você redefinir a senha.</p>

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

            {error && <div className={styles.error}>{error}</div>}

            <button className={styles.submit} type="submit" disabled={submitting}>
              {submitting ? 'Enviando…' : 'Enviar link de redefinição'}
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
