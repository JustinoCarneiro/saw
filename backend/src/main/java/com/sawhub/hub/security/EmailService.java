package com.sawhub.hub.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** H1.4 (M18) — mesmo tratamento do M06 (IA)/M14 (Mercado Pago): sem credencial SMTP
 * configurada neste ambiente, o e-mail não é enviado de verdade — o link é logado em WARN, uma
 * prática comum de dev/staging (ver Suposição 1 do Blueprint M18). {@code JavaMailSender} é
 * injetado como opcional porque a autoconfiguração do Spring Boot só cria o bean quando
 * {@code spring.mail.host} está preenchido. */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailProperties emailProperties;
    private final String frontendBaseUrl;
    private final JavaMailSender mailSender;

    public EmailService(EmailProperties emailProperties,
                         @Value("${sawhub.email.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
                         @Autowired(required = false) JavaMailSender mailSender) {
        this.emailProperties = emailProperties;
        this.frontendBaseUrl = frontendBaseUrl;
        this.mailSender = mailSender;
    }

    public void enviarLinkRedefinicaoSenha(String destinatario, String tokenBruto) {
        String link = frontendBaseUrl + "/redefinir-senha?token=" + tokenBruto;
        String assunto = "SAW HUB — Redefinição de senha";
        String corpo = "Você pediu a redefinição da sua senha no SAW HUB.\n\n"
                + "Acesse o link abaixo pra escolher uma nova senha (válido por 30 minutos):\n" + link + "\n\n"
                + "Se você não pediu isso, pode ignorar este e-mail.";

        if (!emailProperties.isEnabled() || mailSender == null) {
            log.warn("SMTP não configurado (sawhub.email.host vazio) — e-mail de redefinição não enviado de "
                    + "verdade. Destinatário: {}, link: {}", destinatario, link);
            return;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(emailProperties.getRemetente());
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        mailSender.send(mensagem);
    }
}
