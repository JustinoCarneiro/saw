package com.sawhub.hub.security;

/** Lançada quando SMTP não está configurado e o fallback de log (dev/E2E) não está permitido —
 * capturada pelo PasswordResetService, que já trata qualquer RuntimeException de envio sem deixar
 * escapar pro chamador (H1.1: resposta sempre genérica, nunca um oráculo de enumeração de conta). */
public class EmailIndisponivelException extends RuntimeException {
    public EmailIndisponivelException(String message) {
        super(message);
    }
}
