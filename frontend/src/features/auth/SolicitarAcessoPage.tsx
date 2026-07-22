import { type FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import logoSaw from '../../assets/logo-saw.png';
import { apiClient } from '../../shared/lib/apiClient';
import { getApiErrorMessage } from '../../shared/lib/apiError';
import { formatarTelefone } from '../../shared/lib/format';
import styles from './SolicitarAcessoPage.module.css';

export function SolicitarAcessoPage() {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [telefone, setTelefone] = useState('');
  const [mensagem, setMensagem] = useState('');
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
          <img className={styles.logoImg} src={logoSaw} alt="SAW — Escola de Restaurantes" />
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
              onChange={(e) => setTelefone(formatarTelefone(e.target.value))}
              maxLength={15}
            />

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
