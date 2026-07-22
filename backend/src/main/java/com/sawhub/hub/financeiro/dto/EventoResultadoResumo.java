package com.sawhub.hub.financeiro.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Pedido do Marcos (22/07/2026, achado na auditoria de clareza — "métricas de venda de ingresso
 * precisam aparecer também no Financeiro") — mesma riqueza da planilha real "Eventos - Despesas e
 * Receitas" (P&L por evento): Comercial já mostra quantidade/valor vendido por evento
 * ({@code VendaIngressoResumo}), mas o Financeiro não mostrava nenhum resultado (receita − despesa)
 * por evento até agora. */
public record EventoResultadoResumo(
        UUID eventoId,
        String eventoTitulo,
        BigDecimal receitaTotal,
        BigDecimal despesaTotal,
        BigDecimal resultado
) {
}
