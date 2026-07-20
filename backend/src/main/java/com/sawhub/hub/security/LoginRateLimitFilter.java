package com.sawhub.hub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Achado da revisão final de segurança (Fase 5, 20/07/2026): {@code POST /api/v1/auth/login} era
 * o único dos 4 endpoints públicos sem nenhuma proteção contra força bruta/credential stuffing —
 * {@code LeadRateLimitFilter}/{@code AtaAudioRateLimitFilter}/{@code PasswordResetRateLimitFilter}
 * cobrem os outros três.
 * <p>
 * Diferente dos outros três (que contam toda requisição, indistintamente): login legítimo
 * acontece o tempo todo, inclusive vários usuários distintos atrás do mesmo IP (escritório,
 * VPN) — contar toda tentativa igual aos outros filtros bloquearia uso normal. Este filtro só
 * conta TENTATIVAS FALHAS (resposta 401 de {@link AuthFailureHandler}) por IP; um login bem
 * sucedido (200 de {@link AuthSuccessHandler}) zera o contador daquele IP.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Duration JANELA = Duration.ofMinutes(10);
    private static final String PATH = "/api/v1/auth/login";

    private final StringRedisTemplate redisTemplate;
    private final int limite;

    public LoginRateLimitFilter(StringRedisTemplate redisTemplate, @Value("${app.rate-limit.login:5}") int limite) {
        this.redisTemplate = redisTemplate;
        this.limite = limite;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String chave = "loginfailrate:" + request.getRemoteAddr();
        String contagemAtual = redisTemplate.opsForValue().get(chave);
        long falhas = contagemAtual != null ? Long.parseLong(contagemAtual) : 0;
        if (falhas >= limite) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Muitas tentativas de login. Tente novamente mais tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            Long novaContagem = redisTemplate.opsForValue().increment(chave);
            if (novaContagem != null && novaContagem == 1L) {
                redisTemplate.expire(chave, JANELA);
            }
        } else if (response.getStatus() == HttpServletResponse.SC_OK) {
            redisTemplate.delete(chave);
        }
    }
}
