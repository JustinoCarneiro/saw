package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.dto.ConciliacaoVendaResponse;
import com.sawhub.hub.financeiro.CategoriaFinanceira;
import com.sawhub.hub.financeiro.GrupoDre;
import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Change request 17/07/2026 ("conciliação"). M26 repontou de ContaPagarReceber pra
 * LancamentoFinanceiro (merge de entidade, ver ROADMAP.md § "Blueprint (M26)"). */
@ExtendWith(MockitoExtension.class)
class ConciliacaoServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private ParcelaVendaRepository parcelaVendaRepository;

    private ConciliacaoService service() {
        return new ConciliacaoService(leadRepository, parcelaVendaRepository);
    }

    private static Lead leadFechado(String nome, BigDecimal valorTotalVenda, BigDecimal valorPagoNoAto,
                                     BigDecimal taxaPlataformaRetida) {
        Lead lead = new Lead(nome, nome.toLowerCase(java.util.Locale.ROOT) + "@restaurante.com", null, null);
        lead.moverParaEmContato(new com.sawhub.hub.team.Colaborador(null, "Paula", com.sawhub.hub.team.Area.COMERCIAL));
        lead.moverParaProposta();
        lead.fecharVenda(ProdutoVenda.MENTORIA_CONTINUA, OrigemVenda.DIRETA, valorTotalVenda, valorPagoNoAto,
                FormaPagamento.PIX, taxaPlataformaRetida);
        return lead;
    }

    private static CategoriaFinanceira categoria() {
        return new CategoriaFinanceira("Mentoria Contínua", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, null);
    }

    private static LancamentoFinanceiro lancamentoLiquidado(BigDecimal valor) {
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria(), "Parcela",
                valor, LocalDate.of(2026, 8, 1), StatusLancamento.PREVISTO, null, LocalDate.of(2026, 8, 1));
        lancamento.liquidar(LocalDate.of(2026, 7, 20));
        return lancamento;
    }

    private static LancamentoFinanceiro lancamentoParcial(BigDecimal valorTotal, BigDecimal valorPago) {
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria(), "Parcela",
                valorTotal, LocalDate.of(2026, 9, 1), StatusLancamento.PREVISTO, null, LocalDate.of(2026, 9, 1));
        lancamento.liquidarParcial(valorPago, LocalDate.of(2026, 7, 20));
        return lancamento;
    }

    private static LancamentoFinanceiro lancamentoPrevisto(BigDecimal valor) {
        return new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria(), "Parcela", valor,
                LocalDate.of(2026, 10, 1), StatusLancamento.PREVISTO, null, LocalDate.of(2026, 10, 1));
    }

    private static LancamentoFinanceiro lancamentoVencido(BigDecimal valor, LocalDate vencimento) {
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria(), "Parcela",
                valor, vencimento, StatusLancamento.PREVISTO, null, vencimento);
        lancamento.marcarVencida();
        return lancamento;
    }

    @Test
    void listarSomaValorPagoNoAtoMaisTaxaPlataformaSemParcelas() {
        Lead lead = leadFechado("Maria Souza", new BigDecimal("1000.00"), new BigDecimal("890.00"), new BigDecimal("110.00"));
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of(lead));
        when(parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())).thenReturn(List.of());

        List<ConciliacaoVendaResponse> resultado = service().listar();

        assertThat(resultado).hasSize(1);
        ConciliacaoVendaResponse c = resultado.get(0);
        assertThat(c.valorTotalVenda()).isEqualByComparingTo("1000.00");
        assertThat(c.valorRecebido()).isEqualByComparingTo("1000.00");
        assertThat(c.valorPendente()).isEqualByComparingTo("0.00");
        assertThat(c.percentualRecebido()).isEqualTo(100.0);
    }

    @Test
    void listarSomaParcelasLiquidadasIntegralmente() {
        Lead lead = leadFechado("João Silva", new BigDecimal("26000.00"), new BigDecimal("6000.00"), null);
        ParcelaVenda p1 = new ParcelaVenda(lead, 1, new BigDecimal("10000.00"), LocalDate.of(2026, 8, 17));
        p1.vincularLancamento(lancamentoLiquidado(new BigDecimal("10000.00")));
        ParcelaVenda p2 = new ParcelaVenda(lead, 2, new BigDecimal("10000.00"), LocalDate.of(2026, 9, 17));
        p2.vincularLancamento(lancamentoPrevisto(new BigDecimal("10000.00")));
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of(lead));
        when(parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())).thenReturn(List.of(p1, p2));

        List<ConciliacaoVendaResponse> resultado = service().listar();

        ConciliacaoVendaResponse c = resultado.get(0);
        assertThat(c.valorRecebido()).isEqualByComparingTo("16000.00");
        assertThat(c.valorPendente()).isEqualByComparingTo("10000.00");
    }

    @Test
    void listarSomaSoOValorPagoDeParcelaEmLiquidacaoParcial() {
        Lead lead = leadFechado("Ana Costa", new BigDecimal("5000.00"), BigDecimal.ZERO, null);
        ParcelaVenda p1 = new ParcelaVenda(lead, 1, new BigDecimal("5000.00"), LocalDate.of(2026, 8, 17));
        p1.vincularLancamento(lancamentoParcial(new BigDecimal("5000.00"), new BigDecimal("2000.00")));
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of(lead));
        when(parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())).thenReturn(List.of(p1));

        List<ConciliacaoVendaResponse> resultado = service().listar();

        ConciliacaoVendaResponse c = resultado.get(0);
        assertThat(c.valorRecebido()).isEqualByComparingTo("2000.00");
        assertThat(c.valorPendente()).isEqualByComparingTo("3000.00");
        assertThat(c.percentualRecebido()).isEqualTo(40.0);
    }

    @Test
    void listarIgnoraParcelaSemContaVinculadaAinda() {
        Lead lead = leadFechado("Carla Dias", new BigDecimal("3000.00"), BigDecimal.ZERO, null);
        ParcelaVenda semConta = new ParcelaVenda(lead, 1, new BigDecimal("3000.00"), LocalDate.of(2026, 8, 17));
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of(lead));
        when(parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())).thenReturn(List.of(semConta));

        List<ConciliacaoVendaResponse> resultado = service().listar();

        assertThat(resultado.get(0).valorRecebido()).isEqualByComparingTo("0.00");
    }

    // Pedido do Marcos (22/07/2026) — alerta de atraso: parcela com vencimento no passado e
    // ainda não liquidada por completo.
    @Test
    void listarMarcaEmAtrasoParcelaComVencimentoNoPassadoNaoLiquidada() {
        Lead lead = leadFechado("Beatriz Lima", new BigDecimal("10000.00"), BigDecimal.ZERO, null);
        LocalDate vencimento = LocalDate.now().minusDays(15);
        ParcelaVenda p1 = new ParcelaVenda(lead, 1, new BigDecimal("5000.00"), vencimento);
        p1.vincularLancamento(lancamentoVencido(new BigDecimal("5000.00"), vencimento));
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of(lead));
        when(parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())).thenReturn(List.of(p1));

        ConciliacaoVendaResponse c = service().listar().get(0);

        assertThat(c.emAtraso()).isTrue();
        assertThat(c.diasAtraso()).isEqualTo(15);
        assertThat(c.parcelasEmAtraso()).isEqualTo(1);
    }

    // Parcial não vira VENCIDO nunca (LancamentoFinanceiro.marcarVencida só transiciona a partir
    // de PREVISTO) — mesmo assim precisa contar como atraso pela data, não só pelo status.
    @Test
    void listarMarcaEmAtrasoParcelaParcialComVencimentoNoPassado() {
        Lead lead = leadFechado("Carlos Nunes", new BigDecimal("5000.00"), BigDecimal.ZERO, null);
        LocalDate vencimento = LocalDate.now().minusDays(3);
        LancamentoFinanceiro parcial = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoria(), "Parcela",
                new BigDecimal("5000.00"), vencimento, StatusLancamento.PREVISTO, null, vencimento);
        parcial.liquidarParcial(new BigDecimal("2000.00"), LocalDate.now().minusDays(10));
        ParcelaVenda p1 = new ParcelaVenda(lead, 1, new BigDecimal("5000.00"), vencimento);
        p1.vincularLancamento(parcial);
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of(lead));
        when(parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())).thenReturn(List.of(p1));

        ConciliacaoVendaResponse c = service().listar().get(0);

        assertThat(c.emAtraso()).isTrue();
        assertThat(c.diasAtraso()).isEqualTo(3);
    }

    @Test
    void listarSemAtrasoTraDiasEParcelasNulos() {
        Lead lead = leadFechado("Maria Souza", new BigDecimal("1000.00"), new BigDecimal("1000.00"), null);
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of(lead));
        when(parcelaVendaRepository.buscarPorLeadIdComConta(lead.getId())).thenReturn(List.of());

        ConciliacaoVendaResponse c = service().listar().get(0);

        assertThat(c.emAtraso()).isFalse();
        assertThat(c.diasAtraso()).isNull();
        assertThat(c.parcelasEmAtraso()).isNull();
    }

    @Test
    void listarSemVendaFechadaNaoQuebra() {
        when(leadRepository.buscarComVendaFechada()).thenReturn(List.of());

        List<ConciliacaoVendaResponse> resultado = service().listar();

        assertThat(resultado).isEmpty();
    }
}
