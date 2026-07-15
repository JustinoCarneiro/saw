package com.sawhub.hub.loja.pagamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Mesmo padrão do GoogleOAuthProperties (M07): "enabled" é derivado (token não-vazio), não uma
 * flag booleana declarada à parte — single source of truth, nunca duplicar esta checagem em
 * outro lugar. Default vazio de propósito (mesmo raciocínio do bloco `ia` do M06): sem a
 * credencial, o checkout falha limpo com PagamentoIndisponivelException. */
@Component
public class MercadoPagoProperties {

    private final String accessToken;
    private final String webhookSecret;
    private final String baseUrl;

    // baseUrl configurável (default = API real do Mercado Pago) — E2E aponta pro stub local
    // (ver scripts/e2e-mercadopago-stub-server.mjs), mesmo raciocínio de OPENAI_API_BASE_URL.
    public MercadoPagoProperties(@Value("${sawhub.pagamento.mercadopago-access-token:}") String accessToken,
                                  @Value("${sawhub.pagamento.mercadopago-webhook-secret:}") String webhookSecret,
                                  @Value("${sawhub.pagamento.mercadopago-base-url:https://api.mercadopago.com}") String baseUrl) {
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
        this.baseUrl = baseUrl;
    }

    public boolean isEnabled() {
        return !accessToken.isBlank();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
