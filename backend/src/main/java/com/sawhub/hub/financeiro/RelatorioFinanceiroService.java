package com.sawhub.hub.financeiro;

import com.sawhub.hub.common.VariacaoCalculator;
import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.financeiro.dto.CategoriaValor;
import com.sawhub.hub.financeiro.dto.ComparativoMes;
import com.sawhub.hub.financeiro.dto.DashboardFaturamentoResponse;
import com.sawhub.hub.financeiro.dto.DreResponse;
import com.sawhub.hub.financeiro.dto.EventoResultadoResumo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** H14.2 (DRE) + H14.3 (dashboard de faturamento) — só agregação de leitura sobre
 * {@link LancamentoFinanceiro}, sem entidade própria (mesmo padrão do módulo `consolidated/`). */
@Service
public class RelatorioFinanceiroService {

    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final CaixaMensalService caixaMensalService;
    private final EventoRepository eventoRepository;

    public RelatorioFinanceiroService(LancamentoFinanceiroRepository lancamentoRepository,
                                       CaixaMensalService caixaMensalService, EventoRepository eventoRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.caixaMensalService = caixaMensalService;
        this.eventoRepository = eventoRepository;
    }

    public DreResponse dre(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        ResultadoPeriodo atual = calcularResultado(periodo);
        ResultadoPeriodo anterior = calcularResultado(periodo.minusMonths(1));

        BigDecimal resultadoAtual = atual.resultado();
        BigDecimal resultadoAnterior = anterior.resultado();
        double variacaoPct = VariacaoCalculator.pct(resultadoAnterior, resultadoAtual);

        List<LancamentoFinanceiro> lancamentos = lancamentosRealizadosDoPeriodo(periodo);
        BigDecimal despesasFixas = somaPorNatureza(lancamentos, NaturezaFinanceira.FIXA);
        BigDecimal despesasVariaveis = somaPorNatureza(lancamentos, NaturezaFinanceira.VARIAVEL);
        List<CategoriaValor> receitaPorCategoria = somaPorCategoria(lancamentos, TipoLancamento.RECEITA);
        List<CategoriaValor> despesaPorCategoria = somaDespesaPorGrupo(lancamentos);

        return new DreResponse(periodo.toString(), atual.receitaBruta, atual.deducoes, atual.receitaLiquida(),
                atual.custos, atual.despesasOperacionais, despesasFixas, despesasVariaveis, resultadoAtual,
                new ComparativoMes(resultadoAnterior, variacaoPct), receitaPorCategoria, despesaPorCategoria);
    }

    public DashboardFaturamentoResponse dashboardFaturamento(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        List<LancamentoFinanceiro> lancamentos = lancamentosRealizadosDoPeriodo(periodo);

        BigDecimal faturamentoMensal = somaPorTipo(lancamentos, TipoLancamento.RECEITA);

        // Composição por categoria (nome), não por OrigemReceita — ver comentário no DTO. Maior
        // valor primeiro (widget do Dashboard é "de onde vem a maior parte da receita", DRE
        // continua alfabético pra leitura de relatório).
        List<CategoriaValor> composicao = somaPorCategoria(lancamentos, TipoLancamento.RECEITA).stream()
                .sorted(Comparator.comparing(CategoriaValor::valor).reversed())
                .toList();

        BigDecimal mrrAtual = somaMrr(lancamentos);
        BigDecimal mrrAnterior = somaMrr(lancamentosRealizadosDoPeriodo(periodo.minusMonths(1)));

        // Proxy de CHURN DE RECEITA (não de cliente/logo — o modelo atual não rastreia eventos de
        // cancelamento por assinante, só o valor agregado de MRR por período). Só conta queda.
        double churnPct = 0.0;
        if (mrrAnterior.compareTo(BigDecimal.ZERO) > 0 && mrrAtual.compareTo(mrrAnterior) < 0) {
            churnPct = mrrAnterior.subtract(mrrAtual)
                    .divide(mrrAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Pedido do Marcos (22/07/2026) — Dashboard vira resumo das outras abas. resultadoDre
        // reaproveita dre() (mesmo período); saldoCaixaAtual reaproveita CaixaMensalService (mesmo
        // período do picker — Caixa também é conceito mensal). lancamentosPendentes/Vencidos
        // deliberadamente SEM escopo de período: é "o que precisa de atenção agora", não "o que
        // aconteceu nesse mês" — um lançamento vencido de 3 meses atrás continua relevante hoje.
        BigDecimal resultadoDre = dre(ano, mes).resultado();
        BigDecimal saldoCaixaAtual = caixaMensalService.caixaDoMes(ano, mes).totalFinal();
        long lancamentosPendentes = lancamentoRepository.countByStatusIn(
                List.of(StatusLancamento.PREVISTO, StatusLancamento.PARCIAL));
        long lancamentosVencidos = lancamentoRepository.countByStatusIn(List.of(StatusLancamento.VENCIDO));
        List<EventoResultadoResumo> resultadoPorEvento = resultadoPorEvento(ano, mes);

        return new DashboardFaturamentoResponse(faturamentoMensal, mrrAtual, churnPct, composicao,
                resultadoDre, saldoCaixaAtual, lancamentosPendentes, lancamentosVencidos, resultadoPorEvento);
    }

    /** ComercialDashboardService ("Vendas da loja") — {@code origemReceita} continua a chave
     * certa aqui (diferente da composição acima): é uma pergunta de negócio direta ("quanto veio
     * da Loja", que só cobre o módulo E8, hoje pausado), não uma listagem de todas as categorias. */
    public BigDecimal receitaPorOrigem(int ano, int mes, OrigemReceita origem) {
        return lancamentosRealizadosDoPeriodo(YearMonth.of(ano, mes)).stream()
                .filter(l -> l.getTipo() == TipoLancamento.RECEITA)
                .filter(l -> l.getCategoria().getOrigemReceita() == origem)
                .map(LancamentoFinanceiro::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Pedido do Marcos (22/07/2026, achado na auditoria de clareza — "métricas de venda de
    // ingresso precisam aparecer também no Financeiro") — mesma riqueza da planilha real "Eventos
    // - Despesas e Receitas" (P&L por evento). Escopado por evento REALIZADO com dataHora no
    // período (mesmo filtro que ComercialDashboardService.resumoVendaIngresso já usa), mas os
    // lançamentos somados NÃO são escopados por data — uma despesa de evento pode ser paga
    // semanas antes/depois do evento em si, diferente do resto do DRE (que é por dataCompetencia).
    private List<EventoResultadoResumo> resultadoPorEvento(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        Instant inicio = periodo.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant fim = periodo.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return eventoRepository.buscarPorStatusEDataHoraBetween(StatusEvento.REALIZADO, inicio, fim).stream()
                .map(this::resultadoDoEvento)
                .toList();
    }

    private EventoResultadoResumo resultadoDoEvento(Evento evento) {
        List<LancamentoFinanceiro> lancamentos = lancamentoRepository
                .findByEventoIdAndStatus(evento.getId(), StatusLancamento.REALIZADO);
        BigDecimal receita = somaPorTipo(lancamentos, TipoLancamento.RECEITA);
        BigDecimal despesa = somaPorTipo(lancamentos, TipoLancamento.DESPESA);
        return new EventoResultadoResumo(evento.getId(), evento.getTitulo(), receita, despesa, receita.subtract(despesa));
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

    // Gap 6 (Pix Recorrente, confirmado 19/07/2026) — MRR passa a somar OrigemReceita.ASSINATURA
    // OU pagamentoRecorrente=true (ver LancamentoFinanceiro): "recorrente" é informação de
    // negócio da forma de pagamento, não só da categoria do produto (ex.: uma Consultoria paga
    // via Pix Recorrente é receita recorrente de verdade, mesmo sem a categoria "Consultoria" ter
    // origemReceita=ASSINATURA). Filtro OR sobre a mesma lista — sem dupla contagem mesmo quando
    // as duas condições são verdadeiras ao mesmo tempo pro mesmo lançamento.
    private static BigDecimal somaMrr(List<LancamentoFinanceiro> lancamentos) {
        return lancamentos.stream()
                .filter(l -> l.getTipo() == TipoLancamento.RECEITA)
                .filter(l -> l.getCategoria().getOrigemReceita() == OrigemReceita.ASSINATURA || l.isPagamentoRecorrente())
                .map(LancamentoFinanceiro::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<CategoriaValor> somaPorCategoria(List<LancamentoFinanceiro> lancamentos, TipoLancamento tipo) {
        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        for (LancamentoFinanceiro l : lancamentos) {
            if (l.getTipo() != tipo) {
                continue;
            }
            porCategoria.merge(l.getCategoria().getNome(), l.getValor(), BigDecimal::add);
        }
        return porCategoria.entrySet().stream()
                .map(e -> new CategoriaValor(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategoriaValor::categoria))
                .toList();
    }

    // Pedido do Marcos (22/07/2026) — "Despesa por categoria" do DRE precisa bater com a
    // "Categoria" real da planilha (Estrutura/Eventos/Financeiro-Jurídico/Marketing/Operação/
    // Outros/Pessoas, ver V52), não com o nome da subcategoria (ex. "Aluguel") — subcategoria é
    // granularidade de lançamento (Lançamentos), categoria é granularidade de resumo (DRE). Cai
    // pro nome da própria categoria quando `grupo` ainda não foi preenchido (ex. uma categoria
    // criada via "+ Nova categoria" sem grupo definido) — nunca some do gráfico por falta de grupo.
    private static List<CategoriaValor> somaDespesaPorGrupo(List<LancamentoFinanceiro> lancamentos) {
        Map<String, BigDecimal> porGrupo = new LinkedHashMap<>();
        for (LancamentoFinanceiro l : lancamentos) {
            if (l.getTipo() != TipoLancamento.DESPESA) {
                continue;
            }
            String grupo = l.getCategoria().getGrupo();
            String chave = (grupo == null || grupo.isBlank()) ? l.getCategoria().getNome() : grupo;
            porGrupo.merge(chave, l.getValor(), BigDecimal::add);
        }
        return porGrupo.entrySet().stream()
                .map(e -> new CategoriaValor(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CategoriaValor::categoria))
                .toList();
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

    // E14 — só despesa entra na dicotomia fixa/variável (pedido original do cliente era
    // "despesas fixas vs. variáveis"); categoria sem natureza preenchida não soma em nenhuma das
    // duas (ver CategoriaFinanceira.natureza).
    private static BigDecimal somaPorNatureza(List<LancamentoFinanceiro> lancamentos, NaturezaFinanceira natureza) {
        return lancamentos.stream()
                .filter(l -> l.getTipo() == TipoLancamento.DESPESA)
                .filter(l -> l.getCategoria().getNatureza() == natureza)
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
