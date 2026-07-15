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
 * H1.4 (M18), mesmo padrão do {@code LeadRateLimitFilter} (M05): {@code POST
 * /api/v1/auth/esqueci-senha} é um endpoint de escrita sem autenticação — sem limite, um script
 * pode disparar e-mails em massa (custo/spam, quando SMTP real estiver configurado) ou inflar a
 * tabela {@code password_reset_token} indefinidamente. Janela fixa por IP via Redis, mesmos
 * números do LeadRateLimitFilter (5 solicitações a cada 10 minutos). Limite configurável (mesmo
 * motivo do {@code app.rate-limit.lead}): o E2E real (M18, teste de caminho feliz via Mailpit)
 * soma mais de 5 chamadas a este endpoint pela mesma origem numa única rodada da suíte.
 */
@Component
public class PasswordResetRateLimitFilter extends OncePerRequestFilter {

    private static final Duration JANELA = Duration.ofMinutes(10);
    private static final String PATH = "/api/v1/auth/esqueci-senha";

    private final StringRedisTemplate redisTemplate;
    private final int limite;

    public PasswordResetRateLimitFilter(StringRedisTemplate redisTemplate,
                                         @Value("${app.rate-limit.password-reset:5}") int limite) {
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

        String chave = "pwresetrate:" + request.getRemoteAddr();
        Long contagem = redisTemplate.opsForValue().increment(chave);
        if (contagem != null && contagem == 1L) {
            redisTemplate.expire(chave, JANELA);
        }

        if (contagem != null && contagem > limite) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Muitas solicitações. Tente novamente mais tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
