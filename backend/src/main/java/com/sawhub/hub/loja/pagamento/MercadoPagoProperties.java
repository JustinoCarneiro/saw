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

    public MercadoPagoProperties(@Value("${sawhub.pagamento.mercadopago-access-token:}") String accessToken,
                                  @Value("${sawhub.pagamento.mercadopago-webhook-secret:}") String webhookSecret) {
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
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
}
