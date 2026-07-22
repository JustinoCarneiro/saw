package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

// Pedido do Marcos (22/07/2026) — "o Dashboard precisa refletir tudo que está nas outras abas,
// a função de acessar uma aba é só ver em mais detalhe aquilo que já está no Dashboard". Os 4
// campos novos são o resumo de 1 linha de cada aba do Financeiro: resultadoDre (DRE do período
// selecionado), saldoCaixaAtual (Caixa do período selecionado, soma de todas as contas),
// lancamentosPendentes/lancamentosVencidos (Lançamentos — sem escopo de período, é "o que
// precisa de atenção agora"). vendasEmAtraso (Conciliação) NÃO mora aqui — ConciliacaoService
// vive no pacote `comercial`, que já depende de `financeiro`; o inverso criaria ciclo de pacote
// (mesmo raciocínio de LeadService/RelatorioFinanceiroService) — o frontend busca
// GET /admin/financeiro/conciliacao à parte e conta emAtraso ali.
public record DashboardFaturamentoResponse(
        BigDecimal faturamentoMensal,
        BigDecimal mrr,
        double churnPct,
        List<ComposicaoReceita> composicao,
        BigDecimal resultadoDre,
        BigDecimal saldoCaixaAtual,
        long lancamentosPendentes,
        long lancamentosVencidos
) {
}
