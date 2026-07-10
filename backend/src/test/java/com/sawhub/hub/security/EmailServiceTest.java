package com.sawhub.hub.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** H1.4 (M18) — mesmo tratamento do M06/M14: sem credencial, o e-mail não é enviado de verdade
 * (fallback pra log), sem lançar exceção nem quebrar o fluxo de solicitação. */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void semCredencialConfiguradaNaoEnviaEmailNemLancaExcecao() {
        EmailProperties propriedadesDesabilitadas = new EmailProperties("", 587, "", "", "no-reply@x.com");
        EmailService service = new EmailService(propriedadesDesabilitadas, "http://localhost:5173", mailSender);

        service.enviarLinkRedefinicaoSenha("ana@x.com", "token-bruto");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void semMailSenderInjetadoNaoEnviaEmailNemLancaExcecao() {
        EmailProperties propriedadesHabilitadas = new EmailProperties("smtp.x.com", 587, "user", "pass", "no-reply@x.com");
        EmailService service = new EmailService(propriedadesHabilitadas, "http://localhost:5173", null);

        service.enviarLinkRedefinicaoSenha("ana@x.com", "token-bruto");
        // Não lançar exceção já é a asserção — sem JavaMailSender, cai no fallback de log.
    }

    @Test
    void comCredencialConfiguradaEnviaEmailReal() {
        EmailProperties propriedadesHabilitadas = new EmailProperties("smtp.x.com", 587, "user", "pass", "no-reply@x.com");
        EmailService service = new EmailService(propriedadesHabilitadas, "http://localhost:5173", mailSender);

        service.enviarLinkRedefinicaoSenha("ana@x.com", "token-bruto");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
