package com.sawhub.hub.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Fonte única pra saber se o login Google (M07) está configurado — lida tanto pelo
 * {@code SecurityConfig} (decide se registra {@code .oauth2Login()}) quanto pelo
 * {@code AuthController} (expõe {@code GET /auth/oauth2-config} pro frontend só mostrar o botão
 * quando fizer sentido). Nunca duplicar esta checagem em outro lugar. */
@Component
public class GoogleOAuthProperties {

    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthProperties(@Value("${sawhub.oauth2.google-client-id:}") String clientId,
                                  @Value("${sawhub.oauth2.google-client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isEnabled() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
