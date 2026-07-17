package com.sawhub.hub.loja.pagamento;

/** Lançada quando a assinatura HMAC (header x-signature) de uma notificação de webhook do
 * Mercado Pago não bate — distinta de {@link org.springframework.security.access.AccessDeniedException}
 * de propósito (achado ao vivo, suporte do Mercado Pago 2026-07-17): reusar AccessDeniedException
 * fazia o GlobalExceptionHandler devolver "Você não tem acesso a este módulo." pra uma falha de
 * assinatura de webhook, uma mensagem de RBAC administrativo que não tem nada a ver com o caso e
 * atrapalhou o próprio diagnóstico do suporte do MP. */
public class AssinaturaWebhookInvalidaException extends RuntimeException {
    public AssinaturaWebhookInvalidaException(String message) {
        super(message);
    }
}
