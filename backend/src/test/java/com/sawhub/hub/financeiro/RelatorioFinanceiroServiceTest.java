package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** H14.2 (DRE) + H14.3 (dashboard de faturamento) — RED primeiro:
 * RelatorioFinanceiroService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class RelatorioFinanceiroServiceTest {

    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;

    private RelatorioFinanceiroService service() {
        return new RelatorioFinanceiroService(lancamentoRepository);
    }

    private static CategoriaFinanceira categoria(String nome, TipoLancamento tipo, GrupoDre grupo, OrigemReceita origem) {
        return new CategoriaFinanceira(nome, tipo, grupo, origem);
    }

    private static CategoriaFinanceira categoriaComNatureza(String nome, GrupoDre grupo, NaturezaFinanceira natureza) {
        return new CategoriaFinanceira(nome, TipoLancamento.DESPESA, grupo, null, "Estrutura", natureza);
    }

    private static LancamentoFinanceiro lancamento(CategoriaFinanceira categoria, String valor, LocalDate dataCompetencia) {
        return new LancamentoFinanceiro(categoria.getTipo(), categoria, "desc", new BigDecimal(valor),
                dataCompetencia, StatusLancamento.REALIZADO, null);
    }

    @Test
    void dreCalculaHierarquiaCompleta() {
        CategoriaFinanceira assinaturas = categoria("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        CategoriaFinanceira loja = categoria("Loja", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA);
        CategoriaFinanceira impostos = categoria("Impostos", TipoLancamento.DESPESA, GrupoDre.DEDUCOES, null);
        CategoriaFinanceira infra = categoria("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
        CategoriaFinanceira marketing = categoria("Marketing", TipoLancamento.DESPESA, GrupoDre.DESPESA_OPERACIONAL, null);

        LocalDate julho = LocalDate.of(2026, 7, 15);
        LocalDate junho = LocalDate.of(2026, 6, 15);

        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(
                        lancamento(assinaturas, "1000", julho),
                        lancamento(loja, "500", julho),
                        lancamento(impostos, "100", julho),
                        lancamento(infra, "200", julho),
                        lancamento(marketing, "150", julho)));

        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of(
                        lancamento(assinaturas, "900", junho),
                        lancamento(loja, "400", junho),
                        lancamento(impostos, "90", junho),
                        lancamento(infra, "180", junho),
                        lancamento(marketing, "130", junho)));

        var dre = service().dre(2026, 7);

        assertThat(dre.receitaBruta()).isEqualByComparingTo("1500");
        assertThat(dre.deducoes()).isEqualByComparingTo("100");
        assertThat(dre.receitaLiquida()).isEqualByComparingTo("1400");
        assertThat(dre.custos()).isEqualByComparingTo("200");
        assertThat(dre.despesasOperacionais()).isEqualByComparingTo("150");
        // resultado = receitaLiquida(1400) - custos(200) - despesasOperacionais(150) = 1050
        assertThat(dre.resultado()).isEqualByComparingTo("1050");

        // mês anterior: receitaLiquida = (900+400)-90 = 1210; resultado = 1210-180-130 = 900
        assertThat(dre.comparativoMesAnterior().resultado()).isEqualByComparingTo("900");
        // variação: (1050-900)/900 * 100 = 16.67%
        assertThat(dre.comparativoMesAnterior().variacaoPct()).isCloseTo(16.67, org.assertj.core.data.Offset.offset(0.01));
    }

    // E14 — raio-x da planilha real: Fixa/Variável é atributo da subcategoria (CategoriaFinanceira),
    // não escolha livre por lançamento. Categoria sem natureza (ex. ligada a evento) não entra em
    // nenhuma das duas somas, mas continua contando em despesasOperacionais/custos normalmente.
    @Test
    void dreSomaDespesasFixasEVariaveisPorNaturezaDaCategoria() {
        CategoriaFinanceira aluguel = categoriaComNatureza("Aluguel", GrupoDre.CUSTOS, NaturezaFinanceira.FIXA);
        CategoriaFinanceira aguaMineral = categoriaComNatureza("Água Mineral", GrupoDre.CUSTOS, NaturezaFinanceira.VARIAVEL);
        CategoriaFinanceira semNatureza = categoriaComNatureza("Brindes Evento", GrupoDre.DESPESA_OPERACIONAL, null);

        LocalDate julho = LocalDate.of(2026, 7, 15);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(
                        lancamento(aluguel, "2126", julho),
                        lancamento(aguaMineral, "53.82", julho),
                        lancamento(semNatureza, "1608", julho)));
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of());

        var dre = service().dre(2026, 7);

        assertThat(dre.despesasFixas()).isEqualByComparingTo("2126");
        assertThat(dre.despesasVariaveis()).isEqualByComparingTo("53.82");
        // custos = aluguel + aguaMineral (CUSTOS) — semNatureza é DESPESA_OPERACIONAL, soma lá,
        // não em nenhum dos dois "fixa/variável".
        assertThat(dre.custos()).isEqualByComparingTo("2179.82");
        assertThat(dre.despesasOperacionais()).isEqualByComparingTo("1608");
    }

    @Test
    void dreComPeriodoVazioZeraTudoSemDividirPorZero() {
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(eq(StatusLancamento.REALIZADO), any(), any()))
                .thenReturn(List.of());

        var dre = service().dre(2026, 7);

        assertThat(dre.receitaBruta()).isEqualByComparingTo("0");
        assertThat(dre.resultado()).isEqualByComparingTo("0");
        assertThat(dre.comparativoMesAnterior().variacaoPct()).isZero();
    }

    @Test
    void dashboardFaturamentoAgregaComposicaoEMrrSoDeAssinatura() {
        CategoriaFinanceira assinaturas = categoria("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        CategoriaFinanceira loja = categoria("Loja", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA);
        CategoriaFinanceira eventos = categoria("Eventos", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.EVENTO);

        LocalDate julho = LocalDate.of(2026, 7, 15);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(
                        lancamento(assinaturas, "1000", julho),
                        lancamento(loja, "500", julho),
                        lancamento(eventos, "200", julho)));
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of(lancamento(assinaturas, "1000", LocalDate.of(2026, 6, 15))));

        var dashboard = service().dashboardFaturamento(2026, 7);

        assertThat(dashboard.faturamentoMensal()).isEqualByComparingTo("1700");
        assertThat(dashboard.mrr()).isEqualByComparingTo("1000");
        assertThat(dashboard.composicao()).hasSize(3);
        assertThat(dashboard.composicao()).anySatisfy(c -> {
            assertThat(c.origem()).isEqualTo(OrigemReceita.ASSINATURA);
            assertThat(c.valor()).isEqualByComparingTo("1000");
        });
    }

    @Test
    void dashboardFaturamentoCalculaChurnQuandoMrrCai() {
        CategoriaFinanceira assinaturas = categoria("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);

        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(lancamento(assinaturas, "900", LocalDate.of(2026, 7, 15))));
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of(lancamento(assinaturas, "1000", LocalDate.of(2026, 6, 15))));

        var dashboard = service().dashboardFaturamento(2026, 7);

        // (1000 - 900) / 1000 * 100 = 10%
        assertThat(dashboard.churnPct()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.01));
    }

    private static LocalDate any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
