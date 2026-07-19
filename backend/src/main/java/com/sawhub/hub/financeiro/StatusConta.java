package com.sawhub.hub.financeiro;

/** Máquina de estado H14.4 (CLAUDE.md): A pagar/A receber -&gt; Pago/Recebido (ou Vencido).
 * PAGO só é válido pra {@link TipoConta#A_PAGAR}; RECEBIDO só pra {@link TipoConta#A_RECEBER}
 * — a consistência tipo↔status é validada em {@link ContaPagarReceberService}, não no banco,
 * pra dar uma mensagem de erro legível em vez de estourar um CHECK genérico.
 *
 * <p>{@link #PARCIAL} (gap achado via raio-x, 18/07/2026 — ver
 * docs/reuniao-2026-07-17-atualizacoes.md § "Implicações pro desenho", item 1): a planilha real
 * do cliente tem 3 estados de pagamento, não 2. Alcançado só por
 * {@link ContaPagarReceber#liquidarParcial}, que acumula {@code valorPago} até cobrir o valor
 * total (aí a conta liquida por completo, igual antes). */
public enum StatusConta {
    PENDENTE,
    PARCIAL,
    PAGO,
    RECEBIDO,
    VENCIDO
}
