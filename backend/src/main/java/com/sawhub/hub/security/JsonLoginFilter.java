package com.sawhub.hub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Login via JSON ({"email": "...", "senha": "..."}) em vez do form-login padrão do Spring
 * (url-encoded + redirect) — necessário pra um SPA que fala JSON.
 */
public class JsonLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;

    public JsonLoginFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        super(authenticationManager);
        this.objectMapper = objectMapper;
        setFilterProcessesUrl("/api/v1/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequest body = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
            String email = body.email() == null ? "" : body.email().trim().toLowerCase(java.util.Locale.ROOT);
            String senha = body.senha() == null ? "" : body.senha();
            UsernamePasswordAuthenticationToken authRequest =
                    UsernamePasswordAuthenticationToken.unauthenticated(email, senha);
            setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        } catch (java.io.IOException e) {
            throw new org.springframework.security.authentication.InternalAuthenticationServiceException(
                    "Corpo da requisição inválido", e);
        }
    }

    public record LoginRequest(String email, String senha) {
    }
}
