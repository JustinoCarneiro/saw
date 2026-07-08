package com.sawhub.hub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sawhub.hub.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class AuthFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    public AuthFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws java.io.IOException {
        // Mensagem genérica de propósito (H1.1) — nunca revela se foi o e-mail ou a senha que falhou.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(),
                ApiError.of(401, "Unauthorized", "Credenciais inválidas.", request.getRequestURI()));
    }
}
