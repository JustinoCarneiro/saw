package com.sawhub.hub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** H1.4 (M18) — mesmo "Pattern A" de credencial externa opcional já usado em
 * {@code GoogleOAuthProperties}/{@code MercadoPagoProperties}: {@code @Value} com default vazio,
 * {@code isEnabled()} como fonte única de verdade. Achado da revisão final de segurança (Fase 5):
 * sem SMTP configurado, {@code EmailService} só cai no fallback de log (útil em dev — evita
 * precisar de SMTP real pra testar "esqueci minha senha") quando {@code permitirFallbackLog} é
 * explicitamente true; por padrão (produção) lança {@code EmailIndisponivelException} em vez de
 * logar o token de redefinição de senha em texto puro. */
@Component
public class EmailProperties {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String remetente;
    private final boolean permitirFallbackLog;

    public EmailProperties(@Value("${sawhub.email.host:}") String host,
                            @Value("${sawhub.email.port:587}") int port,
                            @Value("${sawhub.email.username:}") String username,
                            @Value("${sawhub.email.password:}") String password,
                            @Value("${sawhub.email.remetente:no-reply@sawhub.com.br}") String remetente,
                            @Value("${sawhub.email.permitir-fallback-log:false}") boolean permitirFallbackLog) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.remetente = remetente;
        this.permitirFallbackLog = permitirFallbackLog;
    }

    public boolean isEnabled() {
        return !host.isBlank();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRemetente() {
        return remetente;
    }

    public boolean isPermitirFallbackLog() {
        return permitirFallbackLog;
    }
}
