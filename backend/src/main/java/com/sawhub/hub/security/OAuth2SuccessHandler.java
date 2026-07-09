package com.sawhub.hub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** H1.2 (M07) — diferente do login e-mail/senha (JsonLoginFilter/AuthSuccessHandler, que
 * respondem JSON pra uma chamada AJAX), o login Google é um redirect de navegador de ponta a
 * ponta: o Google manda o browser de volta pro backend, então a única resposta possível aqui é
 * outro redirect — de volta pro frontend, já autenticado (cookie de sessão setado nesta mesma
 * resposta). O SPA resolve pra onde ir a partir do /auth/me, igual já faz após o login normal. */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final String frontendUrl;

    public OAuth2SuccessHandler(@Value("${sawhub.cors.allowed-origins}") String allowedOrigins) {
        this.frontendUrl = allowedOrigins.split(",")[0].trim();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        response.sendRedirect(frontendUrl + "/login");
    }
}
