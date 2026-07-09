package com.sawhub.hub.loja.pagamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** H8.3 — verificação de assinatura do webhook é o achado mais crítico do módulo: uma assinatura
 * mal validada permite forjar "pagamento aprovado" sem pagar de verdade. O teste positivo
 * recalcula o HMAC de forma independente (não chamando o código de produção pra gerar o valor
 * esperado) — se o manifesto ou o algoritmo mudar por acidente, este teste pega. */
class MercadoPagoGatewayServiceTest {

    private static final String SECRET = "webhook-secret-de-teste";

    private static MercadoPagoGatewayService gateway(String accessToken, String webhookSecret) {
        MercadoPagoProperties properties = new MercadoPagoProperties(accessToken, webhookSecret);
        return new MercadoPagoGatewayService(properties, RestClient.builder());
    }

    private static String tsAgora() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    private static String assinaturaValida(String dataId, String ts, String requestId) {
        try {
            String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "ts=" + ts + ",v1=" + hex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void assinaturaCorretaComputadaComOMesmoAlgoritmoEAceita() {
        MercadoPagoGatewayService gateway = gateway("token", SECRET);
        String dataId = "123456789";
        String requestId = UUID.randomUUID().toString();
        String xSignature = assinaturaValida(dataId, tsAgora(), requestId);

        assertThat(gateway.verificarAssinatura(xSignature, requestId, dataId)).isTrue();
    }

    @Test
    void assinaturaComSecretErradoEhRejeitada() {
        MercadoPagoGatewayService gateway = gateway("token", "outro-secret-completamente-diferente");
        String dataId = "123456789";
        String requestId = UUID.randomUUID().toString();
        String xSignature = assinaturaValida(dataId, tsAgora(), requestId);

        assertThat(gateway.verificarAssinatura(xSignature, requestId, dataId)).isFalse();
    }

    @Test
    void dataIdAdulteradoAposAAssinaturaSerCalculadaEhRejeitado() {
        MercadoPagoGatewayService gateway = gateway("token", SECRET);
        String requestId = UUID.randomUUID().toString();
        String xSignature = assinaturaValida("123456789", tsAgora(), requestId);

        // Mesma assinatura, mas o payment id que o webhook alega ser não bate mais com o que
        // gerou o hash — simula um atacante trocando o id do pagamento numa requisição forjada.
        assertThat(gateway.verificarAssinatura(xSignature, requestId, "999999999")).isFalse();
    }

    @Test
    void requestIdAdulteradoEhRejeitado() {
        MercadoPagoGatewayService gateway = gateway("token", SECRET);
        String dataId = "123456789";
        String xSignature = assinaturaValida(dataId, tsAgora(), UUID.randomUUID().toString());

        assertThat(gateway.verificarAssinatura(xSignature, UUID.randomUUID().toString(), dataId)).isFalse();
    }

    @Test
    void semWebhookSecretConfiguradoSempreRejeita() {
        MercadoPagoGatewayService gateway = gateway("token", "");
        String dataId = "123456789";
        String requestId = UUID.randomUUID().toString();
        String xSignature = assinaturaValida(dataId, tsAgora(), requestId);

        assertThat(gateway.verificarAssinatura(xSignature, requestId, dataId)).isFalse();
    }

    @Test
    void assinaturaComTimestampAntigoEhRejeitadaMesmoValida() {
        MercadoPagoGatewayService gateway = gateway("token", SECRET);
        String dataId = "123456789";
        String requestId = UUID.randomUUID().toString();
        // 10min atrás — fora da janela de 5min (defesa contra replay de uma assinatura capturada).
        String tsAntigo = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());
        String xSignature = assinaturaValida(dataId, tsAntigo, requestId);

        assertThat(gateway.verificarAssinatura(xSignature, requestId, dataId)).isFalse();
    }

    @Test
    void cabecalhosAusentesSaoRejeitados() {
        MercadoPagoGatewayService gateway = gateway("token", SECRET);
        assertThat(gateway.verificarAssinatura(null, "req-1", "123")).isFalse();
        assertThat(gateway.verificarAssinatura("ts=1,v1=abc", null, "123")).isFalse();
        assertThat(gateway.verificarAssinatura("ts=1,v1=abc", "req-1", null)).isFalse();
    }

    @Test
    void semAccessTokenConfiguradoCriarPreferenciaLancaPagamentoIndisponivel() {
        MercadoPagoGatewayService gateway = gateway("", SECRET);
        assertThatThrownBy(() -> gateway.consultarPagamento("pay-1")).isInstanceOf(PagamentoIndisponivelException.class);
    }
}
