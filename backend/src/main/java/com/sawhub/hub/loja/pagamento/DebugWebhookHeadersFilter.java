package com.sawhub.hub.loja.pagamento;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * TEMPORÁRIO — diagnóstico do chamado MP sobre divergência de x-signature (ver CLAUDE.md).
 * Loga a requisição e todos os headers recebidos pelo endpoint do webhook, exatamente como o
 * Spring os viu (sem depender de listar nomes de header manualmente, ao contrário de um log_format
 * do Nginx). Remover depois de capturar uma notificação real.
 */
@Component
public class DebugWebhookHeadersFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DebugWebhookHeadersFilter.class);
    private static final String PATH = "/api/v1/webhooks/mercadopago";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (PATH.equals(request.getRequestURI())) {
            String query = request.getQueryString();
            log.info("MP webhook raw request: {} {}", request.getMethod(),
                    query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query);
            Collections.list(request.getHeaderNames())
                    .forEach(nome -> log.info("MP webhook header: {} = {}", nome, request.getHeader(nome)));
        }
        filterChain.doFilter(request, response);
    }
}
