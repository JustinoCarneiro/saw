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
        // Pedido do Marcos (22/07/2026, achado ao revisar a composição) — por CategoriaFinanceira
        // (nome), não mais por OrigemReceita: origem_receita só cobre ASSINATURA/LOJA/EVENTO/OUTRA
        // (uq_categoria_financeira_origem_receita permite só 1 categoria por origem, ver V22) e
        // deixava "Mentoria Individual"/"Consultoria"/"Patrocínio"/"Produtos Digitais" — categorias
        // reais da planilha, já resolvidas certinho pelo LeadService — invisíveis aqui mesmo
        // quando tinham venda de verdade. "Loja SAW" (módulo pausado) só aparece quando tiver
        // lançamento de fato, não é mais um balde fixo sempre mostrado.
        List<CategoriaValor> composicao,
        BigDecimal resultadoDre,
        BigDecimal saldoCaixaAtual,
        long lancamentosPendentes,
        long lancamentosVencidos,
        // Pedido do Marcos (22/07/2026) — "métricas de venda de ingresso precisam aparecer
        // também no Financeiro": Comercial já mostra quantidade/valor vendido por evento, mas o
        // Financeiro não mostrava nenhum resultado (receita - despesa) por evento até agora.
        List<EventoResultadoResumo> resultadoPorEvento
) {
}
