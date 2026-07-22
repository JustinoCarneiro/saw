package com.sawhub.hub.financeiro;

/** Pedido do Marcos (22/07/2026, achado na auditoria de clareza — "que dados são relevantes pro
 * financeiro a partir dos filtros que podem ser aplicados na planilha dele?") — a planilha real
 * "DRE Financeira Saw" tem uma coluna "Forma de Pagamento" (Pix/Cartão/Boleto) em toda linha de
 * Despesas/Receitas, que o sistema não capturava. Nome distinto de {@code comercial.FormaPagamento}
 * de propósito — mesmos valores reais, mas conceitos em pacotes diferentes (evita qualquer
 * confusão de qual "FormaPagamento" um import não-qualificado se refere); nome comprido também
 * evita ciclo de pacote (financeiro não pode depender de comercial). */
public enum FormaPagamentoLancamento {
    PIX,
    PIX_RECORRENTE,
    CARTAO,
    BOLETO,
    HOTMART
}
