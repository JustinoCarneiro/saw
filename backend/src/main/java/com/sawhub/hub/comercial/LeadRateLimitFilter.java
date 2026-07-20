package com.sawhub.hub.comercial;

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
 * Achado M1 da revisão de segurança do E13: {@code POST /api/v1/leads} é o único endpoint de
 * escrita sem autenticação do sistema (H1.3, "Solicitar acesso") — sem limite, um script pode
 * inflar a tabela {@code lead} indefinidamente, poluindo o funil/dashboard do time comercial.
 * Janela fixa por IP via Redis (já é dependência do projeto pra sessão, não precisa de lib nova):
 * 5 solicitações a cada 10 minutos é generoso pro caso real (visitante reenviando após um erro
 * de digitação), mas limita um flood automatizado.
 * <p>
 * {@code app.rate-limit.lead} é configurável (default 5) só porque a suíte E2E inteira soma mais
 * de 5 submissões reais desse formulário público em specs diferentes (comercial.spec.ts,
 * mentorados.spec.ts, mentorados-comercial-import-export.spec.ts) — todas batendo do mesmo IP
 * (127.0.0.1) dentro da mesma janela de 10min. scripts/e2e-up.sh sobe o backend isolado com um
 * teto bem mais alto; dev/produção continuam no default seguro.
 */
@Component
public class LeadRateLimitFilter extends OncePerRequestFilter {

    private final int limite;
    private static final Duration JANELA = Duration.ofMinutes(10);
    private static final String PATH = "/api/v1/leads";

    private final StringRedisTemplate redisTemplate;

    public LeadRateLimitFilter(StringRedisTemplate redisTemplate, @Value("${app.rate-limit.lead:5}") int limite) {
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

        String chave = "leadrate:" + request.getRemoteAddr();
        Long contagem = redisTemplate.opsForValue().increment(chave);
        if (contagem != null && contagem == 1L) {
            redisTemplate.expire(chave, JANELA);
        }

        if (contagem != null && contagem > limite) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Muitas solicitações. Tente novamente mais tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
