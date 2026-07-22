package com.sawhub.hub.dashboardadmin.dto;

import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.mentorado.TipoContrato;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** H10.1–H10.3 (M16) — painel geral do Admin, distinto do E17 (consolidado por mentorado) e do
 * E13/E14 (dashboards por área). Ver Blueprint M16 no ROADMAP.md pras Suposições sobre
 * "atividades recentes"/"crescimento" serem derivadas por leitura, sem tabela de auditoria. */
public record DashboardAdminResponse(
        long mentoradosAtivos,
        double variacaoMentoradosAtivosPct,
        long mentoriasRealizadas,
        double variacaoMentoriasRealizadasPct,
        long eventosRealizados,
        double variacaoEventosRealizadosPct,
        BigDecimal receitaMes,
        double variacaoReceitaMesPct,
        List<CrescimentoMesItem> crescimentoMentorados,
        List<DistribuicaoTipoContratoItem> distribuicaoTipoContrato,
        List<AtividadeRecente> atividadesRecentes,
        List<MentoriaHojeItem> mentoriasHoje,
        // Pedido do Marcos (22/07/2026) — "o CEO abre só o Dashboard na maioria das vezes":
        // resumo de 1 linha de cada área (Comercial/Financeiro/Caixa/Conciliação) com navegação
        // clicável pra tela de detalhe, mesmo padrão já usado no Dashboard do Financeiro
        // (DashboardFaturamentoResponse "Resumo do Financeiro").
        BigDecimal resultadoDre,
        BigDecimal saldoCaixaAtual,
        long lancamentosPendentes,
        long lancamentosVencidos,
        long leadsEmAberto,
        double taxaConversaoPct,
        long vendasEmAtraso
) {
    public record CrescimentoMesItem(String mes, long total) {
    }

    // tipoContrato nullable de propósito: representa o bucket "Não informado" — mentorados
    // nascidos de vendas sem tipo de contrato de mentoria (ingresso de evento, produto digital
    // etc., ver MentoradoAdminService.mapearTipoContrato) ou dado legado sem essa informação.
    public record DistribuicaoTipoContratoItem(TipoContrato tipoContrato, long quantidade, double pct) {
    }

    public record AtividadeRecente(String tipo, String descricao, Instant quando) {
    }

    public record MentoriaHojeItem(TipoMentoria tipo, String mentorNome, String mentoradoNomes, String hora,
                                    StatusMentoria status) {
    }
}
