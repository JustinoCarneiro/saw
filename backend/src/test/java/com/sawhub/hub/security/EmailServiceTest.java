package com.sawhub.hub.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

/** H1.4 (M18) — mesmo tratamento do M06/M14: sem credencial, o e-mail não é enviado de verdade.
 * Achado da revisão final de segurança (Fase 5): o fallback pra log (útil em dev, evita precisar
 * de SMTP real) só é permitido com permitirFallbackLog=true; por padrão (produção) lança
 * EmailIndisponivelException em vez de logar o token de redefinição em texto puro. */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void semCredencialConfiguradaSemFallbackPermitidoLancaExcecaoSemEnviar() {
        EmailProperties propriedadesDesabilitadas = new EmailProperties("", 587, "", "", "no-reply@x.com", false);
        EmailService service = new EmailService(propriedadesDesabilitadas, "http://localhost:5173", mailSender);

        assertThatThrownBy(() -> service.enviarLinkRedefinicaoSenha("ana@x.com", "token-bruto"))
                .isInstanceOf(EmailIndisponivelException.class);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void semCredencialConfiguradaComFallbackPermitidoNaoEnviaEmailNemLancaExcecao() {
        EmailProperties propriedadesDesabilitadas = new EmailProperties("", 587, "", "", "no-reply@x.com", true);
        EmailService service = new EmailService(propriedadesDesabilitadas, "http://localhost:5173", mailSender);

        service.enviarLinkRedefinicaoSenha("ana@x.com", "token-bruto");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void semMailSenderInjetadoComFallbackPermitidoNaoEnviaEmailNemLancaExcecao() {
        EmailProperties propriedadesHabilitadas = new EmailProperties("smtp.x.com", 587, "user", "pass", "no-reply@x.com", true);
        EmailService service = new EmailService(propriedadesHabilitadas, "http://localhost:5173", null);

        service.enviarLinkRedefinicaoSenha("ana@x.com", "token-bruto");
        // Não lançar exceção já é a asserção — sem JavaMailSender, cai no fallback de log.
    }

    @Test
    void comCredencialConfiguradaEnviaEmailReal() {
        EmailProperties propriedadesHabilitadas = new EmailProperties("smtp.x.com", 587, "user", "pass", "no-reply@x.com", false);
        EmailService service = new EmailService(propriedadesHabilitadas, "http://localhost:5173", mailSender);

        service.enviarLinkRedefinicaoSenha("ana@x.com", "token-bruto");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
