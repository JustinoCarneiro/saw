package com.sawhub.hub.comercial.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Change request 17/07/2026 ("conciliação entre valor total do contrato e valor efetivamente
 * recebido, parcela a parcela — importante pra declaração de imposto"). {@code valorRecebido}
 * soma o que foi pago no ato (+ taxa de plataforma retida, gap 7 — o cliente pagou aquela fatia
 * mesmo que a SAW não tenha ficado com o valor cheio) com as parcelas já liquidadas (PAGO/
 * RECEBIDO conta o valor cheio da parcela; PARCIAL conta só o {@code valorPago} da conta). */
public record ConciliacaoVendaResponse(
        UUID leadId,
        String nome,
        BigDecimal valorTotalVenda,
        BigDecimal valorRecebido,
        BigDecimal valorPendente,
        double percentualRecebido
) {
}
