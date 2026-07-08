package com.sawhub.hub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security 6+ carrega o CSRF token de forma "preguiçosa" (só grava o cookie
 * XSRF-TOKEN se algo efetivamente ler o token durante a requisição). Numa API REST pura, sem
 * views server-side lendo `${_csrf}`, nada dispara essa leitura sozinho — então o cookie nunca
 * seria escrito. Este filtro força a leitura em toda requisição, garantindo que o cookie exista
 * desde a primeira chamada do frontend (mesmo uma que dê 401, como o /me antes do login).
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, java.io.IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
