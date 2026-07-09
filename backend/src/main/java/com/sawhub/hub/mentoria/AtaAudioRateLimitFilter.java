package com.sawhub.hub.mentoria;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** Achado (médio) da revisão de segurança do M06: diferente de {@code /leads} (que já tem
 * {@code LeadRateLimitFilter}), {@code POST .../ata/audio} não tinha limite nenhum — cada
 * chamada dispara uma requisição paga na Whisper API e outra na Claude API, e o próprio código
 * permite reenvio (retry após FALHA, regravar após CONCLUIDO). Sem limite, qualquer conta com
 * MODULO_MENTORADOS pode script-ar uploads repetidos e inflar o custo de infra sem limite algum
 * (risco de orçamento já sinalizado no ROADMAP.md M06, mas sem proteção equivalente até aqui).
 * Chave por usuário autenticado (não por IP, ao contrário do LeadRateLimitFilter) — este endpoint
 * já exige login, então IP é um proxy pior pra identidade real do que a sessão autenticada. */
@Component
public class AtaAudioRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE = 10;
    private static final Duration JANELA = Duration.ofHours(1);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String PATTERN = "/api/v1/admin/mentorias/*/ata/audio";

    private final StringRedisTemplate redisTemplate;

    public AtaAudioRateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !PATH_MATCHER.match(PATTERN, request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identidade = auth != null ? auth.getName() : request.getRemoteAddr();
        String chave = "atarate:" + identidade;
        Long contagem = redisTemplate.opsForValue().increment(chave);
        if (contagem != null && contagem == 1L) {
            redisTemplate.expire(chave, JANELA);
        }

        if (contagem != null && contagem > LIMITE) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Muitos envios de áudio. Tente novamente mais tarde.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
