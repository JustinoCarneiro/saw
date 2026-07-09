package com.sawhub.hub.loja;

import com.sawhub.hub.loja.pagamento.MercadoPagoGatewayService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** H8.3-H8.4 (M14) — endpoint PÚBLICO (sem sessão, liberado explicitamente em SecurityConfig),
 * protegido por verificação de assinatura HMAC do Mercado Pago, não por autenticação de usuário
 * (o Mercado Pago nunca tem sessão SAW HUB). Assinatura inválida -&gt; 403, nunca processa. */
@RestController
@RequestMapping("/api/v1/webhooks/mercadopago")
public class WebhookMercadoPagoController {

    private final MercadoPagoGatewayService gateway;
    private final PedidoPagamentoService pedidoPagamentoService;

    public WebhookMercadoPagoController(MercadoPagoGatewayService gateway, PedidoPagamentoService pedidoPagamentoService) {
        this.gateway = gateway;
        this.pedidoPagamentoService = pedidoPagamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void receber(@RequestParam(value = "data.id", required = false) String dataId,
                         @RequestParam(required = false) String type,
                         @RequestHeader(value = "x-signature", required = false) String xSignature,
                         @RequestHeader(value = "x-request-id", required = false) String xRequestId) {
        if (dataId == null || !"payment".equals(type)) {
            return; // outros tipos de notificação (ex.: merchant_order) — só payment interessa aqui
        }
        if (!gateway.verificarAssinatura(xSignature, xRequestId, dataId)) {
            throw new AccessDeniedException("Assinatura do webhook inválida.");
        }
        pedidoPagamentoService.processarNotificacao(dataId);
    }
}
