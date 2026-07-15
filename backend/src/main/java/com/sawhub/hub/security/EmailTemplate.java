package com.sawhub.hub.security;

/** Casca HTML compartilhada pelos e-mails transacionais do SAW HUB. Cores e forma do botão
 * primário replicam {@code design/tokens.css} (--wine, --gold, --on-gold, pílula) — mesma
 * identidade visual do app, não uma paleta inventada pro e-mail. Layout em tabelas + estilo
 * inline de propósito: é o único jeito de ter aparência consistente em clientes de e-mail
 * (Outlook usa o motor de renderização do Word, ignora flexbox/grid e a maior parte do CSS3). */
final class EmailTemplate {

    static final String LOGO_CONTENT_ID = "logoSaw";

    private EmailTemplate() {
    }

    static String corpo(String titulo, String mensagem, String textoBotao, String linkBotao, String notaRodape) {
        return "<!doctype html>"
                + "<html lang=\"pt-BR\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + titulo + "</title></head>"
                + "<body style=\"margin:0;padding:0;background-color:#EFE7DA;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#EFE7DA;padding:32px 16px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"width:560px;max-width:100%;background-color:#FFFFFF;border-radius:16px;overflow:hidden;\">"

                // Faixa de marca (vinho) com a logo
                + "<tr><td align=\"center\" bgcolor=\"#57191C\" "
                + "style=\"background-color:#57191C;background-image:linear-gradient(135deg,#3C1013 0%,#57191C 55%,#7A2328 100%);padding:36px 24px;\">"
                + "<img src=\"cid:" + LOGO_CONTENT_ID + "\" width=\"140\" alt=\"SAW HUB\" "
                + "style=\"display:block;width:140px;max-width:60%;height:auto;\">"
                + "</td></tr>"

                // Corpo
                + "<tr><td style=\"padding:40px 40px 8px 40px;font-family:'Inter',system-ui,-apple-system,"
                + "'Segoe UI',Roboto,sans-serif;\">"
                + "<h1 style=\"margin:0 0 16px 0;font-size:22px;line-height:1.3;color:#3C1013;\">" + titulo + "</h1>"
                + "<p style=\"margin:0 0 28px 0;font-size:15px;line-height:1.6;color:#45403B;\">" + mensagem + "</p>"
                + "</td></tr>"

                // Botão CTA
                + "<tr><td align=\"center\" style=\"padding:0 40px 32px 40px;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr><td "
                + "style=\"border-radius:999px;background-color:#F0B050;\">"
                + "<a href=\"" + linkBotao + "\" target=\"_blank\" "
                + "style=\"display:inline-block;padding:14px 32px;font-family:'Inter',system-ui,-apple-system,"
                + "'Segoe UI',Roboto,sans-serif;font-size:15px;font-weight:600;color:#1A1206;text-decoration:none;"
                + "border-radius:999px;\">" + textoBotao + "</a>"
                + "</td></tr></table>"
                + "</td></tr>"

                // Link alternativo (caso o botão não renderize)
                + "<tr><td style=\"padding:0 40px 32px 40px;font-family:'Inter',system-ui,-apple-system,"
                + "'Segoe UI',Roboto,sans-serif;\">"
                + "<p style=\"margin:0;font-size:13px;line-height:1.6;color:#8A8078;\">"
                + "Se o botão não funcionar, copie e cole este link no navegador:<br>"
                + "<a href=\"" + linkBotao + "\" target=\"_blank\" style=\"color:#7A2328;word-break:break-all;\">"
                + linkBotao + "</a></p>"
                + "</td></tr>"

                // Divisor + nota de rodapé
                + "<tr><td style=\"padding:0 40px;\"><hr style=\"border:none;border-top:1px solid #EDE6DA;margin:0;\"></td></tr>"
                + "<tr><td style=\"padding:24px 40px 32px 40px;font-family:'Inter',system-ui,-apple-system,"
                + "'Segoe UI',Roboto,sans-serif;\">"
                + "<p style=\"margin:0;font-size:13px;line-height:1.6;color:#8A8078;\">" + notaRodape + "</p>"
                + "</td></tr>"

                + "</table>"

                // Assinatura fora do cartão
                + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:560px;max-width:100%;\">"
                + "<tr><td align=\"center\" style=\"padding:20px 24px 0 24px;font-family:'Inter',system-ui,"
                + "-apple-system,'Segoe UI',Roboto,sans-serif;\">"
                + "<p style=\"margin:0;font-size:12px;color:#A9A29A;\">SAW HUB &middot; Escola de Restaurante</p>"
                + "</td></tr></table>"

                + "</td></tr></table>"
                + "</body></html>";
    }
}
