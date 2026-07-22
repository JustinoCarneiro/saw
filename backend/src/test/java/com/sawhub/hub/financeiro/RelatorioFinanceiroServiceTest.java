package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.StatusEvento;
import com.sawhub.hub.financeiro.dto.CaixaMensalResponse;
import java.math.BigDecimal;
import java.time.Instant;
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
    @Mock
    private CaixaMensalService caixaMensalService;
    @Mock
    private EventoRepository eventoRepository;

    private RelatorioFinanceiroService service() {
        return new RelatorioFinanceiroService(lancamentoRepository, caixaMensalService, eventoRepository);
    }

    // Pedido do Marcos (22/07/2026) — dashboardFaturamento() passou a reaproveitar
    // CaixaMensalService.caixaDoMes(); testes que não são sobre Caixa em si só precisam de um
    // retorno não-nulo pra não estourar NPE em totalFinal().
    private void stubCaixaVazio() {
        when(caixaMensalService.caixaDoMes(anyInt(), anyInt()))
                .thenReturn(new CaixaMensalResponse(2026, 7, List.of(), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    // Nota: dashboardFaturamento() também chama eventoRepository.buscarPorStatusEDataHoraBetween
    // (pro "resultado por evento") — testes que não são sobre isso não precisam stubar: Mockito
    // devolve List vazia por padrão pra @Mock não-stubado (ReturnsEmptyValues), sem NPE.

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

    private static LancamentoFinanceiro lancamentoRecorrente(CategoriaFinanceira categoria, String valor, LocalDate dataCompetencia) {
        return new LancamentoFinanceiro(categoria.getTipo(), categoria, "desc", new BigDecimal(valor),
                dataCompetencia, StatusLancamento.REALIZADO, null, null, true);
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
        stubCaixaVazio();
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
            assertThat(c.categoria()).isEqualTo("Assinaturas");
            assertThat(c.valor()).isEqualByComparingTo("1000");
        });
        // Maior valor primeiro (widget do Dashboard, ver comentário em dashboardFaturamento()).
        assertThat(dashboard.composicao()).extracting("categoria")
                .containsExactly("Assinaturas", "Loja", "Eventos");
    }

    // Pedido do Marcos (22/07/2026) — achado ao revisar a composição: categorias reais da
    // planilha sem OrigemReceita (Mentoria Individual, Consultoria, Produtos Digitais, Patrocínio
    // — uq_categoria_financeira_origem_receita só permite 1 categoria por origem, ver V22) ficavam
    // fora da composição inteira mesmo tendo venda de verdade. Agora entram por nome de categoria.
    @Test
    void dashboardFaturamentoComposicaoInclueCategoriaSemOrigemReceita() {
        stubCaixaVazio();
        CategoriaFinanceira mentoriaIndividual = categoria("Mentoria Individual", TipoLancamento.RECEITA,
                GrupoDre.RECEITA_BRUTA, null);
        CategoriaFinanceira produtosDigitais = categoria("Produtos Digitais", TipoLancamento.RECEITA,
                GrupoDre.RECEITA_BRUTA, null);

        LocalDate julho = LocalDate.of(2026, 7, 15);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(
                        lancamento(mentoriaIndividual, "300", julho),
                        lancamento(produtosDigitais, "900", julho)));

        var dashboard = service().dashboardFaturamento(2026, 7);

        assertThat(dashboard.composicao()).hasSize(2);
        // Maior valor primeiro: Produtos Digitais (900) antes de Mentoria Individual (300).
        assertThat(dashboard.composicao()).extracting("categoria")
                .containsExactly("Produtos Digitais", "Mentoria Individual");
    }

    @Test
    void receitaPorOrigemSomaSoALinhaDaOrigemPedida() {
        CategoriaFinanceira assinaturas = categoria("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        CategoriaFinanceira loja = categoria("Loja", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.LOJA);
        LocalDate julho = LocalDate.of(2026, 7, 15);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(lancamento(assinaturas, "1000", julho), lancamento(loja, "500", julho)));

        assertThat(service().receitaPorOrigem(2026, 7, OrigemReceita.LOJA)).isEqualByComparingTo("500");
        assertThat(service().receitaPorOrigem(2026, 7, OrigemReceita.EVENTO)).isEqualByComparingTo("0");
    }

    @Test
    void dashboardFaturamentoCalculaChurnQuandoMrrCai() {
        stubCaixaVazio();
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

    // Gap 6 (Pix Recorrente, confirmado 19/07/2026) — receita fora de OrigemReceita.ASSINATURA
    // (ex.: Consultoria, categoria sem origemReceita) mas paga via Pix Recorrente também soma no
    // MRR: "recorrente" é sinal da forma de pagamento, não só da categoria do produto.
    @Test
    void dashboardFaturamentoMrrSomaPagamentoRecorrenteMesmoForaDeAssinatura() {
        stubCaixaVazio();
        CategoriaFinanceira assinaturas = categoria("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        CategoriaFinanceira consultoria = categoria("Consultoria", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, null);

        LocalDate julho = LocalDate.of(2026, 7, 15);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(
                        lancamento(assinaturas, "1000", julho),
                        lancamentoRecorrente(consultoria, "500", julho),
                        lancamento(consultoria, "300", julho))); // não recorrente — não deve somar no MRR
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of());

        var dashboard = service().dashboardFaturamento(2026, 7);

        // MRR = 1000 (assinatura) + 500 (consultoria paga via Pix Recorrente) = 1500; os 300 da
        // consultoria não-recorrente ficam de fora.
        assertThat(dashboard.mrr()).isEqualByComparingTo("1500");
        assertThat(dashboard.faturamentoMensal()).isEqualByComparingTo("1800");
    }

    // "mais gráficos e detalhe que estão nas planilhas do financeiro" (reunião 17/07/2026) —
    // breakdown por categoria real, mesma granularidade da planilha "DRE Financeira Saw".
    @Test
    void dreExpoeReceitaEDespesaPorCategoria() {
        CategoriaFinanceira mentoriaContinua = categoria("Mentoria Contínua", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        CategoriaFinanceira eventos = categoria("Eventos", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.EVENTO);
        CategoriaFinanceira infra = categoria("Infraestrutura", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);

        LocalDate julho = LocalDate.of(2026, 7, 15);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(
                        lancamento(mentoriaContinua, "1000", julho),
                        lancamento(mentoriaContinua, "500", julho),
                        lancamento(eventos, "200", julho),
                        lancamento(infra, "150", julho)));
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of());

        var dre = service().dre(2026, 7);

        assertThat(dre.receitaPorCategoria()).hasSize(2);
        assertThat(dre.receitaPorCategoria()).anySatisfy(c -> {
            assertThat(c.categoria()).isEqualTo("Mentoria Contínua");
            assertThat(c.valor()).isEqualByComparingTo("1500");
        });
        assertThat(dre.receitaPorCategoria()).anySatisfy(c -> {
            assertThat(c.categoria()).isEqualTo("Eventos");
            assertThat(c.valor()).isEqualByComparingTo("200");
        });
        assertThat(dre.despesaPorCategoria()).hasSize(1);
        assertThat(dre.despesaPorCategoria().get(0).categoria()).isEqualTo("Infraestrutura");
        assertThat(dre.despesaPorCategoria().get(0).valor()).isEqualByComparingTo("150");
    }

    // Pedido do Marcos (22/07/2026) — "Despesa por categoria" precisa agrupar pela Categoria real
    // da planilha (Estrutura/Pessoas/Eventos/... — V52), não pela subcategoria: duas subcategorias
    // diferentes (ex. "Aluguel"/"Água Mineral") sob o mesmo grupo ("Estrutura") somam juntas.
    @Test
    void dreAgrupaDespesaPorGrupoQuandoPreenchidoEmVezDeSubcategoria() {
        CategoriaFinanceira aluguel = new CategoriaFinanceira("Aluguel", TipoLancamento.DESPESA,
                GrupoDre.DESPESA_OPERACIONAL, null, "Estrutura", null);
        CategoriaFinanceira aguaMineral = new CategoriaFinanceira("Água Mineral", TipoLancamento.DESPESA,
                GrupoDre.DESPESA_OPERACIONAL, null, "Estrutura", null);
        CategoriaFinanceira diretor = new CategoriaFinanceira("Diretor", TipoLancamento.DESPESA,
                GrupoDre.DESPESA_OPERACIONAL, null, "Pessoas", null);
        // Categoria sem grupo preenchido (ex.: criada via "+ Nova categoria" sem definir grupo)
        // cai pro próprio nome, nunca some do gráfico.
        CategoriaFinanceira semGrupo = new CategoriaFinanceira("Categoria livre", TipoLancamento.DESPESA,
                GrupoDre.DESPESA_OPERACIONAL, null);

        LocalDate julho = LocalDate.of(2026, 7, 15);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(List.of(
                        lancamento(aluguel, "2000", julho),
                        lancamento(aguaMineral, "50", julho),
                        lancamento(diretor, "8000", julho),
                        lancamento(semGrupo, "300", julho)));
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(
                eq(StatusLancamento.REALIZADO), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(List.of());

        var dre = service().dre(2026, 7);

        assertThat(dre.despesaPorCategoria()).hasSize(3);
        assertThat(dre.despesaPorCategoria()).anySatisfy(c -> {
            assertThat(c.categoria()).isEqualTo("Estrutura");
            assertThat(c.valor()).isEqualByComparingTo("2050"); // Aluguel (2000) + Água Mineral (50)
        });
        assertThat(dre.despesaPorCategoria()).anySatisfy(c -> {
            assertThat(c.categoria()).isEqualTo("Pessoas");
            assertThat(c.valor()).isEqualByComparingTo("8000");
        });
        assertThat(dre.despesaPorCategoria()).anySatisfy(c -> {
            assertThat(c.categoria()).isEqualTo("Categoria livre");
            assertThat(c.valor()).isEqualByComparingTo("300");
        });
    }

    // Pedido do Marcos (22/07/2026) — "o Dashboard precisa refletir tudo que está nas outras
    // abas": resultadoDre reaproveita dre(), saldoCaixaAtual reaproveita CaixaMensalService,
    // lancamentosPendentes/Vencidos contam por status sem escopo de período.
    @Test
    void dashboardFaturamentoExpoeResumoDeDreCaixaELancamentos() {
        CategoriaFinanceira assinaturas = categoria("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(eq(StatusLancamento.REALIZADO), any(), any()))
                .thenReturn(List.of(lancamento(assinaturas, "1000", LocalDate.of(2026, 7, 15))));
        when(caixaMensalService.caixaDoMes(2026, 7))
                .thenReturn(new CaixaMensalResponse(2026, 7, List.of(), new BigDecimal("5000"), new BigDecimal("7500")));
        when(lancamentoRepository.countByStatusIn(List.of(StatusLancamento.PREVISTO, StatusLancamento.PARCIAL)))
                .thenReturn(4L);
        when(lancamentoRepository.countByStatusIn(List.of(StatusLancamento.VENCIDO))).thenReturn(2L);

        var dashboard = service().dashboardFaturamento(2026, 7);

        // resultadoDre = receitaBruta(1000) - deducoes(0) - custos(0) - despesaOperacional(0) = 1000.
        assertThat(dashboard.resultadoDre()).isEqualByComparingTo("1000");
        assertThat(dashboard.saldoCaixaAtual()).isEqualByComparingTo("7500");
        assertThat(dashboard.lancamentosPendentes()).isEqualTo(4L);
        assertThat(dashboard.lancamentosVencidos()).isEqualTo(2L);
        assertThat(dashboard.resultadoPorEvento()).isEmpty();
    }

    // Pedido do Marcos (22/07/2026, achado na auditoria de clareza — "métricas de venda de
    // ingresso precisam aparecer também no Financeiro") — mesma riqueza da planilha real "Eventos
    // - Despesas e Receitas": receita/despesa/resultado por evento REALIZADO no período.
    @Test
    void dashboardFaturamentoAgregaResultadoPorEvento() {
        stubCaixaVazio();
        CategoriaFinanceira eventos = categoria("Eventos", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.EVENTO);
        CategoriaFinanceira estrutura = categoria("Estrutura Evento", TipoLancamento.DESPESA, GrupoDre.DESPESA_OPERACIONAL, null);
        when(lancamentoRepository.findByStatusAndDataCompetenciaBetween(eq(StatusLancamento.REALIZADO), any(), any()))
                .thenReturn(List.of());
        when(lancamentoRepository.countByStatusIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(0L);

        com.sawhub.hub.evento.Evento evento = new com.sawhub.hub.evento.Evento("Receita do Sucesso",
                com.sawhub.hub.evento.TipoEvento.PRESENCIAL, null, Instant.parse("2026-07-10T19:00:00Z"),
                "Recife", null, 200);
        java.util.UUID eventoId = java.util.UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(evento, "id", eventoId);
        when(eventoRepository.buscarPorStatusEDataHoraBetween(eq(StatusEvento.REALIZADO),
                org.mockito.ArgumentMatchers.any(Instant.class), org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(evento));

        LancamentoFinanceiro receita1 = lancamento(eventos, "600", LocalDate.of(2026, 7, 10));
        LancamentoFinanceiro receita2 = lancamento(eventos, "400", LocalDate.of(2026, 7, 10));
        LancamentoFinanceiro despesa = lancamento(estrutura, "300", LocalDate.of(2026, 6, 20)); // paga antes do evento
        when(lancamentoRepository.findByEventoIdAndStatus(eventoId, StatusLancamento.REALIZADO))
                .thenReturn(List.of(receita1, receita2, despesa));

        var dashboard = service().dashboardFaturamento(2026, 7);

        assertThat(dashboard.resultadoPorEvento()).hasSize(1);
        var resumo = dashboard.resultadoPorEvento().get(0);
        assertThat(resumo.eventoId()).isEqualTo(eventoId);
        assertThat(resumo.eventoTitulo()).isEqualTo("Receita do Sucesso");
        assertThat(resumo.receitaTotal()).isEqualByComparingTo("1000");
        assertThat(resumo.despesaTotal()).isEqualByComparingTo("300");
        assertThat(resumo.resultado()).isEqualByComparingTo("700");
    }

    private static LocalDate any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
