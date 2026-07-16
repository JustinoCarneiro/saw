import { type FormEvent, useEffect, useState } from 'react';
import { isAxiosError } from 'axios';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import logoSaw from '../../assets/logo-saw.png';
import { useAuth } from './AuthContext';
import { apiClient } from '../../shared/lib/apiClient';
import { firstPermittedRoute } from '../../shared/lib/moduloRoutes';
import type { OAuth2ConfigResponse } from '../../shared/lib/types';
import styles from './LoginPage.module.css';

const OAUTH_ERROR_LABEL: Record<string, string> = {
  email_nao_verificado: 'Seu e-mail do Google não está verificado. Tente outra conta ou entre com e-mail e senha.',
  // Mensagem propositalmente genérica (H1.1) — não confirma nem nega se existe conta SAW HUB
  // pra esse e-mail (achado do revisor-seguranca: oráculo de enumeração de contas).
  login_nao_permitido: 'Não foi possível entrar com essa conta Google. Tente e-mail e senha ou solicite acesso abaixo.',
  erro_desconhecido: 'Não foi possível entrar com o Google agora. Tente novamente.',
};

export function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [googleEnabled, setGoogleEnabled] = useState(false);
  const isDemoMode = new URLSearchParams(window.location.search).get('demo') === '1';

  useEffect(() => {
    apiClient.get<OAuth2ConfigResponse>('/auth/oauth2-config')
      .then((res) => setGoogleEnabled(res.data.googleEnabled))
      .catch(() => setGoogleEnabled(false));
  }, []);

  useEffect(() => {
    const codigo = new URLSearchParams(window.location.search).get('erroOAuth');
    if (codigo) {
      setError(OAUTH_ERROR_LABEL[codigo] ?? OAUTH_ERROR_LABEL.erro_desconhecido);
    }
  }, []);

  if (!loading && user?.perfil === 'ADMIN') {
    return <Navigate to={firstPermittedRoute(user.modulosPermitidos)} replace />;
  }

  // M08 — /mentorado existe de verdade agora (Dashboard, H2.1–H2.3). Antes disso (M06/M07) só
  // havia um placeholder de "área em construção" aqui.
  if (!loading && user?.perfil === 'MENTORADO') {
    return <Navigate to="/mentorado" replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const loggedInUser = await login(email, senha);
      // firstPermittedRoute só faz sentido pra ADMIN (modulosPermitidos é RBAC por área, E15) —
      // MENTORADO não tem módulos, teria caído de volta em /login (modulosPermitidos vazio).
      navigate(loggedInUser.perfil === 'MENTORADO' ? '/mentorado' : firstPermittedRoute(loggedInUser.modulosPermitidos));
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
              <img className={styles.logoImg} src={logoSaw} alt="SAW — Escola de Restaurantes" />
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
            <Link className={styles.forgotPasswordLink} to="/esqueci-senha">
              Esqueci minha senha
            </Link>

            {error && <div className={styles.error}>{error}</div>}

            <button className={styles.submit} type="submit" disabled={submitting}>
              {submitting ? 'Entrando…' : 'Entrar'}
            </button>
            {googleEnabled && (
              // Redirect de navegador de verdade (não uma chamada do apiClient) — o fluxo OAuth2
              // precisa sair da SPA e ir pro Google, não dá pra fazer via AJAX.
              <a className={styles.outlineButton} href="/oauth2/authorization/google">
                Entrar com Google
              </a>
            )}
            <Link className={styles.outlineButton} to="/solicitar-acesso">
              Solicitar acesso
            </Link>

            {isDemoMode && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 16 }}>
                <div style={{ fontSize: 12, color: 'var(--text-soft)', textTransform: 'uppercase', fontWeight: 600 }}>Acesso rápido (Demo)</div>
                <button type="button" className={styles.outlineButton} onClick={() => { setEmail('justinocarneiro161@gmail.com'); setSenha('7OstFrPSu4UHUHARddV6jKwo'); }}>Preencher Admin (Fundador)</button>
                <button type="button" className={styles.outlineButton} onClick={() => { setEmail('lucas@sawhub.com.br'); setSenha('trocar-no-primeiro-login'); }}>Preencher Colaborador (Gestão)</button>
                <button type="button" className={styles.outlineButton} onClick={() => { setEmail('paula@sawhub.com.br'); setSenha('trocar-no-primeiro-login'); }}>Preencher Colaborador (Comercial)</button>
                <button type="button" className={styles.outlineButton} onClick={() => { setEmail('joao@saborearte.com.br'); setSenha('trocar-no-primeiro-login'); }}>Preencher Mentorado (João)</button>
              </div>
            )}
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
