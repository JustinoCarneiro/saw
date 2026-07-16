package com.sawhub.hub.loja.pagamento;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * TEMPORÁRIO — diagnóstico do chamado MP sobre divergência de x-signature (ver CLAUDE.md).
 * Loga a requisição, todos os headers e o corpo (body) recebidos pelo endpoint do webhook,
 * exatamente como o Spring os viu — o controller nunca lê o body (só data.id/type via
 * @RequestParam da query string), então o campo user_id do payload nunca tinha sido observado
 * até agora. Lê o InputStream cru diretamente (ContentCachingRequestWrapper só cacheia o que é
 * efetivamente lido por outra camada, e nada a jusante lê o body aqui — ficava sempre vazio).
 * Remover depois de capturar uma notificação real.
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

        byte[] body = request.getInputStream().readAllBytes();
        log.info("MP webhook body ({} bytes): {}", body.length, new String(body, StandardCharsets.UTF_8));

        filterChain.doFilter(new ReplayedBodyRequest(request, body), response);
    }

    /** Substitui o InputStream já consumido por um novo, replay dos mesmos bytes — o controller
     * não lê o body hoje, mas evita quebrar qualquer coisa que viesse a ler depois no chain. */
    private static final class ReplayedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        ReplayedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return buffer.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return buffer.read();
                }
            };
        }
    }
}
