package com.sawhub.hub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sawhub.hub.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Sem isso, uma chamada não-autenticada a uma rota protegida cai no comportamento padrão do
 * Spring Security (redirect 302 pra uma página de login HTML) — o fetch do SPA segue o redirect
 * e recebe "200 OK" cheio de HTML, extremamente confuso de depurar. Aqui devolve 401 JSON direto.
 */
@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(),
                ApiError.of(401, "Unauthorized", "Faça login para continuar.", request.getRequestURI()));
    }
}
