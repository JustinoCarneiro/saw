// Cliente mínimo da API HTTP do Mailpit (captura SMTP local, ver docker-compose.yml e
// scripts/e2e-up.sh) — deixa os testes E2E lerem um e-mail que o backend realmente enviou por
// SMTP, em vez de confiar só no fallback de log (EMAIL_PERMITIR_FALLBACK_LOG). É o mesmo tipo de
// dependência de canal externo do OAuth do Google e do Mercado Pago: sem isso, o caminho feliz de
// qualquer fluxo por e-mail é opaco pro Playwright.

const MAILPIT_BASE_URL = process.env.MAILPIT_BASE_URL ?? 'http://localhost:8025';

type MailpitMessageSummary = {
  ID: string;
  Subject: string;
  To: { Address: string }[];
};

type MailpitMessageDetail = {
  Text: string;
};

/** Espera (poll, até 10s) o e-mail mais recente pra `destinatario` cujo assunto contenha
 * `assuntoContem`, e devolve o link que casa com `linkRegex` no corpo em texto puro. */
export async function esperarLinkNoUltimoEmail(
  destinatario: string,
  assuntoContem: string,
  linkRegex: RegExp,
): Promise<string> {
  const prazo = Date.now() + 10_000;
  let ultimoErro: unknown;

  while (Date.now() < prazo) {
    try {
      const lista = await fetch(`${MAILPIT_BASE_URL}/api/v1/messages?limit=50`).then(
        (r) => r.json() as Promise<{ messages: MailpitMessageSummary[] }>,
      );
      const msg = lista.messages.find(
        (m) => m.Subject.includes(assuntoContem) && m.To?.some((to) => to.Address === destinatario),
      );
      if (msg) {
        const detalhe = await fetch(`${MAILPIT_BASE_URL}/api/v1/message/${msg.ID}`).then(
          (r) => r.json() as Promise<MailpitMessageDetail>,
        );
        const match = detalhe.Text.match(linkRegex);
        if (match) return match[0];
      }
    } catch (erro) {
      ultimoErro = erro;
    }
    await new Promise((resolve) => setTimeout(resolve, 300));
  }

  throw new Error(
    `Nenhum e-mail com assunto contendo "${assuntoContem}" pra ${destinatario} encontrado no Mailpit em 10s`
        + (ultimoErro ? ` (último erro: ${ultimoErro})` : ''),
  );
}
