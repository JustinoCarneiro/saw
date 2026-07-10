package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.VariacaoCalculator;
import com.sawhub.hub.financeiro.dto.ComparativoMes;
import com.sawhub.hub.financeiro.dto.ComposicaoReceita;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import com.sawhub.hub.financeiro.dto.DreResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** H14.2 (DRE) + H14.3 (dashboard de faturamento) — só agregação de leitura sobre
 * {@link LancamentoFinanceiro}, sem entidade própria (mesmo padrão do módulo `consolidated/`). */
@Service
public class RelatorioFinanceiroService {

    private final LancamentoFinanceiroRepository lancamentoRepository;

    public RelatorioFinanceiroService(LancamentoFinanceiroRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    public DreResponse dre(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        ResultadoPeriodo atual = calcularResultado(periodo);
        ResultadoPeriodo anterior = calcularResultado(periodo.minusMonths(1));

        BigDecimal resultadoAtual = atual.resultado();
        BigDecimal resultadoAnterior = anterior.resultado();
        double variacaoPct = VariacaoCalculator.pct(resultadoAnterior, resultadoAtual);

        return new DreResponse(periodo.toString(), atual.receitaBruta, atual.deducoes, atual.receitaLiquida(),
                atual.custos, atual.despesasOperacionais, resultadoAtual,
                new ComparativoMes(resultadoAnterior, variacaoPct));
    }

    public DashboardFaturamentoResponse dashboardFaturamento(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        List<LancamentoFinanceiro> lancamentos = lancamentosRealizadosDoPeriodo(periodo);

        BigDecimal faturamentoMensal = somaPorTipo(lancamentos, TipoLancamento.RECEITA);

        Map<OrigemReceita, BigDecimal> porOrigem = new EnumMap<>(OrigemReceita.class);
        for (LancamentoFinanceiro l : lancamentos) {
            if (l.getTipo() != TipoLancamento.RECEITA || l.getCategoria().getOrigemReceita() == null) {
                continue;
            }
            porOrigem.merge(l.getCategoria().getOrigemReceita(), l.getValor(), BigDecimal::add);
        }
        List<ComposicaoReceita> composicao = porOrigem.entrySet().stream()
                .map(e -> new ComposicaoReceita(e.getKey(), e.getValue()))
                .toList();

        BigDecimal mrrAtual = porOrigem.getOrDefault(OrigemReceita.ASSINATURA, BigDecimal.ZERO);
        BigDecimal mrrAnterior = somaOrigemAssinatura(lancamentosRealizadosDoPeriodo(periodo.minusMonths(1)));

        // Proxy de CHURN DE RECEITA (não de cliente/logo — o modelo atual não rastreia eventos de
        // cancelamento por assinante, só o valor agregado de MRR por período). Só conta queda.
        double churnPct = 0.0;
        if (mrrAnterior.compareTo(BigDecimal.ZERO) > 0 && mrrAtual.compareTo(mrrAnterior) < 0) {
            churnPct = mrrAnterior.subtract(mrrAtual)
                    .divide(mrrAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return new DashboardFaturamentoResponse(faturamentoMensal, mrrAtual, churnPct, composicao);
    }

    private List<LancamentoFinanceiro> lancamentosRealizadosDoPeriodo(YearMonth periodo) {
        return lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                StatusLancamento.REALIZADO, periodo.atDay(1), periodo.atEndOfMonth());
    }

    private ResultadoPeriodo calcularResultado(YearMonth periodo) {
        List<LancamentoFinanceiro> lancamentos = lancamentosRealizadosDoPeriodo(periodo);
        return new ResultadoPeriodo(
                somaPorGrupo(lancamentos, GrupoDre.RECEITA_BRUTA),
                somaPorGrupo(lancamentos, GrupoDre.DEDUCOES),
                somaPorGrupo(lancamentos, GrupoDre.CUSTOS),
                somaPorGrupo(lancamentos, GrupoDre.DESPESA_OPERACIONAL));
    }

    private static BigDecimal somaOrigemAssinatura(List<LancamentoFinanceiro> lancamentos) {
        return lancamentos.stream()
                .filter(l -> l.getTipo() == TipoLancamento.RECEITA)
                .filter(l -> l.getCategoria().getOrigemReceita() == OrigemReceita.ASSINATURA)
                .map(LancamentoFinanceiro::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal somaPorGrupo(List<LancamentoFinanceiro> lancamentos, GrupoDre grupo) {
        return lancamentos.stream()
                .filter(l -> l.getCategoria().getGrupoDre() == grupo)
                .map(LancamentoFinanceiro::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal somaPorTipo(List<LancamentoFinanceiro> lancamentos, TipoLancamento tipo) {
        return lancamentos.stream()
                .filter(l -> l.getTipo() == tipo)
                .map(LancamentoFinanceiro::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ResultadoPeriodo(BigDecimal receitaBruta, BigDecimal deducoes, BigDecimal custos,
                                     BigDecimal despesasOperacionais) {
        BigDecimal receitaLiquida() {
            return receitaBruta.subtract(deducoes);
        }

        BigDecimal resultado() {
            return receitaLiquida().subtract(custos).subtract(despesasOperacionais);
        }
    }
}
