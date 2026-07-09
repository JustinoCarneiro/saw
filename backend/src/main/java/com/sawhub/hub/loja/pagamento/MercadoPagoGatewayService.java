package com.sawhub.hub.loja.pagamento;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sawhub.hub.loja.ItemPedido;
import com.sawhub.hub.loja.Pedido;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** H8.3 (M14) — checkout via Checkout Pro (Preferences API): o mentorado é redirecionado pro
 * checkout hospedado pelo Mercado Pago, o SAW HUB nunca processa dado de cartão diretamente.
 * Chamada HTTP direta via RestClient (não o SDK oficial) — mesmo padrão já usado em
 * WhisperTranscricaoService/ClaudeAtaRascunhoService (M06): timeout explícito, exceção
 * sentinela própria, falha limpa sem credencial. Ver Suposições do Blueprint M14 no ROADMAP.md. */
@Service
public class MercadoPagoGatewayService {

    private static final String PREFERENCES_ENDPOINT = "https://api.mercadopago.com/checkout/preferences";
    private static final String PAYMENTS_ENDPOINT = "https://api.mercadopago.com/v1/payments/";

    private final MercadoPagoProperties properties;
    private final RestClient restClient;

    public MercadoPagoGatewayService(MercadoPagoProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public PreferenciaCriada criarPreferencia(Pedido pedido, String backUrlSucesso, String backUrlFalha,
                                               String backUrlPendente, String notificationUrl) {
        exigirConfigurado();
        List<Map<String, Object>> itens = pedido.getItens().stream().map(MercadoPagoGatewayService::paraItemPreferencia).toList();
        Map<String, Object> body = Map.of(
                "items", itens,
                "external_reference", pedido.getId().toString(),
                "back_urls", Map.of("success", backUrlSucesso, "failure", backUrlFalha, "pending", backUrlPendente),
                "notification_url", notificationUrl,
                "auto_return", "approved"
        );

        try {
            PreferenceResponse resposta = restClient.post()
                    .uri(PREFERENCES_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PreferenceResponse.class);
            if (resposta == null || resposta.initPoint() == null) {
                throw new PagamentoIndisponivelException("Resposta vazia da API do Mercado Pago ao criar preferência.");
            }
            return new PreferenciaCriada(resposta.initPoint(), resposta.id());
        } catch (PagamentoIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new PagamentoIndisponivelException("Falha ao criar checkout no Mercado Pago.", e);
        }
    }

    /** Nunca confia no corpo da notificação do webhook — sempre re-consulta o Payment de verdade
     * na API do Mercado Pago a partir só do id recebido (padrão documentado pelo próprio gateway
     * contra notificações forjadas, ver Suposições do Blueprint M14). */
    public PagamentoConsultado consultarPagamento(String paymentId) {
        exigirConfigurado();
        try {
            PaymentResponse resposta = restClient.get()
                    .uri(PAYMENTS_ENDPOINT + paymentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .retrieve()
                    .body(PaymentResponse.class);
            if (resposta == null) {
                throw new PagamentoIndisponivelException("Resposta vazia da API do Mercado Pago ao consultar pagamento " + paymentId + ".");
            }
            return new PagamentoConsultado(resposta.status(), resposta.externalReference());
        } catch (PagamentoIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new PagamentoIndisponivelException("Falha ao consultar pagamento " + paymentId + " no Mercado Pago.", e);
        }
    }

    // Achado (baixo) do revisor-seguranca no M14: janela de frescor pro replay de uma assinatura
    // capturada — defesa em profundidade, não a proteção principal (que já é o HMAC em si).
    private static final long JANELA_TS_SEGUNDOS = Duration.ofMinutes(5).toSeconds();

    /** Verificação de assinatura do webhook (header {@code x-signature: ts=...,v1=...} +
     * {@code x-request-id}), algoritmo documentado publicamente pelo Mercado Pago: HMAC-SHA256
     * sobre o manifesto {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;}. Implementação de
     * boa-fé a partir da documentação pública — validar contra o sandbox real do Mercado Pago
     * antes de qualquer demo/produção que dependa do webhook (mesmo "verificado só até a borda"
     * do pipeline de IA do M06, ver ROADMAP.md). */
    public boolean verificarAssinatura(String xSignature, String xRequestId, String dataId) {
        if (properties.getWebhookSecret().isBlank() || xSignature == null || xRequestId == null || dataId == null) {
            return false;
        }
        Map<String, String> partes = parseXSignature(xSignature);
        String ts = partes.get("ts");
        String v1Recebido = partes.get("v1");
        if (ts == null || v1Recebido == null || !dentroDaJanela(ts)) {
            return false;
        }
        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        String v1Calculado = hmacSha256Hex(manifest, properties.getWebhookSecret());
        return MessageDigest.isEqual(
                v1Calculado.getBytes(StandardCharsets.UTF_8),
                v1Recebido.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean dentroDaJanela(String ts) {
        try {
            long tsSegundos = Long.parseLong(ts);
            long agora = Instant.now().getEpochSecond();
            return Math.abs(agora - tsSegundos) <= JANELA_TS_SEGUNDOS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void exigirConfigurado() {
        if (!properties.isEnabled()) {
            throw new PagamentoIndisponivelException("MERCADOPAGO_ACCESS_TOKEN não configurado — checkout indisponível.");
        }
    }

    private static Map<String, Object> paraItemPreferencia(ItemPedido item) {
        return Map.of(
                "title", item.getProduto().getTitulo(),
                "quantity", item.getQuantidade(),
                "unit_price", item.getPrecoUnitario(),
                "currency_id", "BRL"
        );
    }

    private static Map<String, String> parseXSignature(String header) {
        Map<String, String> partes = new HashMap<>();
        for (String parte : header.split(",")) {
            String[] kv = parte.split("=", 2);
            if (kv.length == 2) {
                partes.put(kv[0].trim(), kv[1].trim());
            }
        }
        return partes;
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new PagamentoIndisponivelException("Falha ao calcular assinatura HMAC do webhook.", e);
        }
    }

    public record PagamentoConsultado(String status, String pedidoId) {
    }

    public record PreferenciaCriada(String initPoint, String preferenceId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PreferenceResponse(@JsonProperty("init_point") String initPoint, String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentResponse(String status, @JsonProperty("external_reference") String externalReference) {
    }
}
