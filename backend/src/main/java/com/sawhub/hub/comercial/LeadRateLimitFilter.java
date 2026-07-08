package com.sawhub.hub.comercial;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Achado M1 da revisão de segurança do E13: {@code POST /api/v1/leads} é o único endpoint de
 * escrita sem autenticação do sistema (H1.3, "Solicitar acesso") — sem limite, um script pode
 * inflar a tabela {@code lead} indefinidamente, poluindo o funil/dashboard do time comercial.
 * Janela fixa por IP via Redis (já é dependência do projeto pra sessão, não precisa de lib nova):
 * 5 solicitações a cada 10 minutos é generoso pro caso real (visitante reenviando após um erro
 * de digitação), mas limita um flood automatizado.
 */
@Component
public class LeadRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE = 5;
    private static final Duration JANELA = Duration.ofMinutes(10);
    private static final String PATH = "/api/v1/leads";

    private final StringRedisTemplate redisTemplate;

    public LeadRateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String chave = "leadrate:" + request.getRemoteAddr();
        Long contagem = redisTemplate.opsForValue().increment(chave);
        if (contagem != null && contagem == 1L) {
            redisTemplate.expire(chave, JANELA);
        }

        if (contagem != null && contagem > LIMITE) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Muitas solicitações. Tente novamente mais tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
