import { type FormEvent, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { apiClient } from '../../shared/lib/apiClient';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import styles from './SolicitarAcessoPage.module.css';

export function RedefinirSenhaPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [redefinida, setRedefinida] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (novaSenha !== confirmarSenha) {
      setError('As senhas não coincidem.');
      return;
    }

    setSubmitting(true);
    try {
      await apiClient.post('/auth/redefinir-senha', { token, novaSenha });
      setRedefinida(true);
    } catch (err) {
      // Mensagem genérica do backend (token inválido/expirado/já usado, sem distinguir qual).
      setError(getApiErrorMessage(err, 'Link inválido ou expirado. Solicite um novo.'));
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

        {redefinida ? (
          <div className={styles.confirmacao} data-testid="redefinir-senha-confirmacao">
            <h1 className={styles.headline}>Senha redefinida.</h1>
            <p className={styles.confirmacaoTexto}>Você já pode entrar com sua nova senha.</p>
            <Link className={styles.submit} to="/login">
              Ir para o login
            </Link>
          </div>
        ) : !token ? (
          <div className={styles.confirmacao} data-testid="redefinir-senha-sem-token">
            <h1 className={styles.headline}>Link inválido.</h1>
            <p className={styles.confirmacaoTexto}>Esse link de redefinição não é válido. Solicite um novo.</p>
            <Link className={styles.submit} to="/esqueci-senha">
              Solicitar novo link
            </Link>
          </div>
        ) : (
          <form className={styles.form} onSubmit={handleSubmit}>
            <h1 className={styles.headline}>Nova senha</h1>
            <p className={styles.subtitle}>Escolha uma nova senha pra sua conta.</p>

            <label className={styles.label} htmlFor="novaSenha">Nova senha</label>
            <input
              id="novaSenha"
              className={styles.input}
              type="password"
              value={novaSenha}
              onChange={(e) => setNovaSenha(e.target.value)}
              minLength={8}
              autoComplete="new-password"
              required
            />

            <label className={styles.label} htmlFor="confirmarSenha">Confirmar nova senha</label>
            <input
              id="confirmarSenha"
              className={styles.input}
              type="password"
              value={confirmarSenha}
              onChange={(e) => setConfirmarSenha(e.target.value)}
              minLength={8}
              autoComplete="new-password"
              required
            />

            {error && <div className={styles.error}>{error}</div>}

            <button className={styles.submit} type="submit" disabled={submitting}>
              {submitting ? 'Salvando…' : 'Redefinir senha'}
            </button>
            <Link className={styles.outlineButton} to="/login">
              Cancelar
            </Link>
          </form>
        )}
      </div>
    </div>
  );
}
