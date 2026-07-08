import { type FormEvent, useState } from 'react';
import { isAxiosError } from 'axios';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { firstPermittedRoute } from '../../shared/lib/moduloRoutes';
import styles from './LoginPage.module.css';

export function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!loading && user?.perfil === 'ADMIN') {
    return <Navigate to={firstPermittedRoute(user.modulosPermitidos)} replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const loggedInUser = await login(email, senha);
      navigate(firstPermittedRoute(loggedInUser.modulosPermitidos));
    } catch (err) {
      // 401 é o único caso de "credenciais erradas" de verdade — qualquer outro status
      // (403 de CORS/CSRF, 5xx, falha de rede) é um problema de infra, não do usuário,
      // e mostrar a mesma mensagem genérica só esconde o que realmente quebrou.
      if (isAxiosError(err) && err.response?.status === 401) {
        setError('E-mail ou senha inválidos.');
      } else {
        setError('Não foi possível entrar agora. Tente novamente em instantes.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.grid}>
          <form className={styles.form} onSubmit={handleSubmit}>
            <div className={styles.logoBlock}>
              <svg width="44" height="44" viewBox="0 0 44 44" fill="none">
                <rect x="22" y="4" width="25.5" height="25.5" rx="4" transform="rotate(45 22 4)" stroke="#F0B050" strokeWidth="1.6" />
                <rect x="22" y="13" width="12.7" height="12.7" rx="2" transform="rotate(45 22 13)" stroke="#F0B050" strokeWidth="1.4" />
              </svg>
              <div className={styles.wordmark}>SAW</div>
              <div className={styles.eyebrow}>ESCOLA DE RESTAURANTES</div>
            </div>

            <h1 className={styles.headline}>
              Formando a nova
              <br />
              <span className={styles.headlineAccent}>geração de gestores.</span>
            </h1>

            <label className={styles.label} htmlFor="email">
              E-mail
            </label>
            <input
              id="email"
              className={styles.input}
              type="email"
              placeholder="seu@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              required
            />

            <label className={styles.label} htmlFor="senha">
              Senha
            </label>
            <div className={styles.passwordWrap}>
              <input
                id="senha"
                className={styles.input}
                type={showPw ? 'text' : 'password'}
                placeholder="Digite sua senha"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                autoComplete="current-password"
                required
              />
              <button
                type="button"
                aria-label="Mostrar senha"
                className={styles.togglePw}
                onClick={() => setShowPw((v) => !v)}
              >
                {showPw ? 'Ocultar' : 'Ver'}
              </button>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            <button className={styles.submit} type="submit" disabled={submitting}>
              {submitting ? 'Entrando…' : 'Entrar'}
            </button>
            <Link className={styles.outlineButton} to="/solicitar-acesso">
              Solicitar acesso
            </Link>
          </form>

          <div className={styles.hero}>
            <div className={styles.heroQuoteMark}>&ldquo;</div>
            <div className={styles.heroQuote}>
              Um bom gestor
              <br />
              não apaga incêndios.
              <br />
              <span className={styles.heroQuoteAccent}>
                Ele evita que o
                <br />
                fogo comece.
              </span>
            </div>
            <div className={styles.heroAuthor}>MATHEUS BRAYAN</div>
            <div className={styles.heroRole}>FUNDADOR DA SAW</div>
          </div>
        </div>
        <div className={styles.footer}>
          <span>© 2026 SAW HUB. Todos os direitos reservados.</span>
        </div>
      </div>
    </div>
  );
}
