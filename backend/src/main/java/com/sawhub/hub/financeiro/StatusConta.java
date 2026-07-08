package com.sawhub.hub.financeiro;

/** Máquina de estado H14.4 (CLAUDE.md): A pagar/A receber -&gt; Pago/Recebido (ou Vencido).
 * PAGO só é válido pra {@link TipoConta#A_PAGAR}; RECEBIDO só pra {@link TipoConta#A_RECEBER}
 * — a consistência tipo↔status é validada em {@link ContaPagarReceberService}, não no banco,
 * pra dar uma mensagem de erro legível em vez de estourar um CHECK genérico. */
public enum StatusConta {
    PENDENTE,
    PAGO,
    RECEBIDO,
    VENCIDO
}
