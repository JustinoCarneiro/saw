package com.sawhub.hub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** H1.2 (M07) — falha (e-mail não verificado ou sem {@link Usuario} correspondente, ver
 * {@link GoogleOAuth2UserService}) redireciona pro login com um código de erro que o frontend
 * traduz numa mensagem — nunca vaza detalhe técnico do OAuth2 pro usuário final. */
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final String frontendUrl;

    public OAuth2FailureHandler(@Value("${sawhub.cors.allowed-origins}") String allowedOrigins) {
        this.frontendUrl = allowedOrigins.split(",")[0].trim();
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        String codigo = "erro_desconhecido";
        if (exception instanceof OAuth2AuthenticationException oauthEx && oauthEx.getError().getErrorCode() != null) {
            codigo = oauthEx.getError().getErrorCode();
        }
        String encoded = URLEncoder.encode(codigo, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/login?erroOAuth=" + encoded);
    }
}
