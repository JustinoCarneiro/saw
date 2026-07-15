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
    private final String authorizationUri;
    private final String tokenUri;
    private final String userInfoUri;

    // URIs configuráveis (default = endpoints reais do Google): E2E aponta pra um stub de IdP
    // local (ver scripts/e2e-oauth-stub-server.mjs) — mesmo raciocínio de MAIL_HOST/OPENAI_API_BASE_URL
    // apontando pros stubs de SMTP/IA, sem tocar no comportamento de produção (default preserva).
    public GoogleOAuthProperties(@Value("${sawhub.oauth2.google-client-id:}") String clientId,
                                  @Value("${sawhub.oauth2.google-client-secret:}") String clientSecret,
                                  @Value("${sawhub.oauth2.google-authorization-uri:https://accounts.google.com/o/oauth2/v2/auth}") String authorizationUri,
                                  @Value("${sawhub.oauth2.google-token-uri:https://www.googleapis.com/oauth2/v4/token}") String tokenUri,
                                  @Value("${sawhub.oauth2.google-user-info-uri:https://www.googleapis.com/oauth2/v3/userinfo}") String userInfoUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
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

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }
}
