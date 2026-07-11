package com.sawhub.hub.dashboardadmin.dto;

import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.mentorado.Plano;
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
        List<DistribuicaoPlanoItem> distribuicaoPlano,
        List<AtividadeRecente> atividadesRecentes,
        List<MentoriaHojeItem> mentoriasHoje,
        // M23 — sparkline nos KPIs (pedido do Marcos: "cliente é bastante visual"). Campos
        // aditivos, não tocam em nenhum contrato existente (Diretiva Primária, CLAUDE.md).
        List<SparklinePonto> historicoMentoriasRealizadas,
        List<SparklinePonto> historicoEventosRealizados,
        List<SparklinePonto> historicoReceitaMes
) {
    public record CrescimentoMesItem(String mes, long total) {
    }

    public record DistribuicaoPlanoItem(Plano plano, long quantidade, double pct) {
    }

    public record AtividadeRecente(String tipo, String descricao, Instant quando) {
    }

    public record MentoriaHojeItem(TipoMentoria tipo, String mentorNome, String mentoradoNomes, String hora,
                                    StatusMentoria status) {
    }

    // M23 — mesma janela de 6 meses de CrescimentoMesItem, mas genérico o bastante pra qualquer
    // métrica de sparkline (contagem ou valor monetário) sem precisar de um record por métrica.
    public record SparklinePonto(String mes, double valor) {
    }
}
