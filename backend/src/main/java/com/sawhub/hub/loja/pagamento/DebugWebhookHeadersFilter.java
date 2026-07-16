package com.sawhub.hub.loja.pagamento;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * TEMPORÁRIO — diagnóstico do chamado MP sobre divergência de x-signature (ver CLAUDE.md).
 * Loga a requisição, todos os headers e o corpo (body) recebidos pelo endpoint do webhook,
 * exatamente como o Spring os viu — o controller nunca lê o body (só data.id/type via
 * @RequestParam da query string), então o campo user_id do payload nunca tinha sido observado
 * até agora. ContentCachingRequestWrapper permite ler o body aqui sem quebrar o restante do
 * filter chain. Remover depois de capturar uma notificação real.
 */
@Component
public class DebugWebhookHeadersFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DebugWebhookHeadersFilter.class);
    private static final String PATH = "/api/v1/webhooks/mercadopago";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        String query = request.getQueryString();
        log.info("MP webhook raw request: {} {}", request.getMethod(),
                query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query);
        Collections.list(request.getHeaderNames())
                .forEach(nome -> log.info("MP webhook header: {} = {}", nome, request.getHeader(nome)));

        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        filterChain.doFilter(wrapped, response);
        byte[] body = wrapped.getContentAsByteArray();
        log.info("MP webhook body ({} bytes): {}", body.length, new String(body, StandardCharsets.UTF_8));
    }
}
