package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.evento.Evento;
import com.sawhub.hub.evento.EventoRepository;
import com.sawhub.hub.evento.TipoEvento;
import com.sawhub.hub.financeiro.dto.CriarLancamentoRequest;
import com.sawhub.hub.financeiro.dto.LiquidarLancamentoRequest;
import com.sawhub.hub.financeiro.dto.LiquidarParcialLancamentoRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** H14.1 + H14.4 — M26 fundiu {@code ContaPagarReceberService} aqui (merge de entidade, ver
 * ROADMAP.md § "Blueprint (M26)"): este arquivo absorveu os casos de teste equivalentes de
 * {@code ContaPagarReceberServiceTest} (removido). */
@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;
    @Mock
    private EventoRepository eventoRepository;

    private LancamentoService service() {
        return new LancamentoService(lancamentoRepository, categoriaRepository, eventoRepository);
    }

    // Sentinela pro filtro de período "desligado" — mesma janela usada por LancamentoService
    // (ver comentário em LancamentoFinanceiroRepository.buscarComFiltroPorVencimento).
    private static final LocalDate SEM_FILTRO_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SEM_FILTRO_FIM = LocalDate.of(2999, 12, 31);

    private static CategoriaFinanceira categoriaAssinatura() {
        return new CategoriaFinanceira("Mentoria Contínua", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
    }

    private static CategoriaFinanceira categoriaInfra() {
        return new CategoriaFinanceira("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
    }

    @Test
    void criarRejeitaCategoriaInexistente() {
        UUID categoriaId = UUID.randomUUID();
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.empty());
        var request = new CriarLancamentoRequest(TipoLancamento.RECEITA, categoriaId, "Assinatura João Silva",
                new BigDecimal("397.00"), LocalDate.of(2026, 7, 1), StatusLancamento.REALIZADO);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria");
    }

    @Test
    void criarPersisteLancamentoComOsDadosDaRequest() {
        UUID categoriaId = UUID.randomUUID();
        CategoriaFinanceira categoria = categoriaAssinatura();
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarLancamentoRequest(TipoLancamento.RECEITA, categoriaId, "Assinatura João Silva",
                new BigDecimal("397.00"), LocalDate.of(2026, 7, 1), StatusLancamento.REALIZADO);

        LancamentoFinanceiro criado = service().criar(request);

        assertThat(criado.getTipo()).isEqualTo(TipoLancamento.RECEITA);
        assertThat(criado.getCategoria()).isEqualTo(categoria);
        assertThat(criado.getDescricao()).isEqualTo("Assinatura João Silva");
        assertThat(criado.getValor()).isEqualByComparingTo("397.00");
        assertThat(criado.getStatus()).isEqualTo(StatusLancamento.REALIZADO);
        assertThat(criado.getDataVencimento()).isNull();
    }

    // M26 — CriarLancamentoRequest ganhou dataVencimento/eventoId (absorvidos de CriarContaRequest).
    @Test
    void criarComEventoIdEDataVencimentoResolveEPersisteOsDois() {
        UUID categoriaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        CategoriaFinanceira categoria = categoriaInfra();
        Evento evento = new Evento("Receita do Sucesso", TipoEvento.PRESENCIAL, null, Instant.now(), "Recife", null, 100);
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarLancamentoRequest(TipoLancamento.DESPESA, categoriaId, "Buffet do evento",
                new BigDecimal("2000.00"), LocalDate.of(2026, 8, 20), StatusLancamento.PREVISTO, eventoId,
                LocalDate.of(2026, 8, 25));
        LancamentoFinanceiro criado = service().criar(request);

        assertThat(criado.getEvento()).isSameAs(evento);
        assertThat(criado.getDataVencimento()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void criarComEventoIdInexistenteLancaErro() {
        UUID categoriaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoriaInfra()));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.empty());

        var request = new CriarLancamentoRequest(TipoLancamento.DESPESA, categoriaId, "Buffet do evento",
                new BigDecimal("2000.00"), LocalDate.of(2026, 8, 20), StatusLancamento.PREVISTO, eventoId, null);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evento");
    }

    @Test
    void listarPorCompetenciaPropagaFiltrosPraORepositorio() {
        UUID categoriaId = UUID.randomUUID();
        LocalDate de = LocalDate.of(2026, 7, 1);
        LocalDate ate = LocalDate.of(2026, 7, 31);
        when(lancamentoRepository.buscarComFiltroPorCompetencia(TipoLancamento.RECEITA, categoriaId, null, null, null, de, ate))
                .thenReturn(List.of());

        service().listar(de, ate, TipoLancamento.RECEITA, categoriaId);

        verify(lancamentoRepository).buscarComFiltroPorCompetencia(TipoLancamento.RECEITA, categoriaId, null, null, null, de, ate);
    }

    // M26 — status/eventoId novos no filtro de /lancamentos (absorvidos de Contas).
    @Test
    void listarPorCompetenciaComStatusEEventoIdPropagaOsDois() {
        UUID eventoId = UUID.randomUUID();
        LocalDate de = LocalDate.of(2026, 7, 1);
        LocalDate ate = LocalDate.of(2026, 7, 31);
        when(lancamentoRepository.buscarComFiltroPorCompetencia(null, null, StatusLancamento.PARCIAL, eventoId, null, de, ate))
                .thenReturn(List.of());

        service().listar(de, ate, null, null, StatusLancamento.PARCIAL, eventoId, null);

        verify(lancamentoRepository).buscarComFiltroPorCompetencia(null, null, StatusLancamento.PARCIAL, eventoId, null, de, ate);
    }

    // Pedido do Marcos (22/07/2026) — formaPagamento novo no filtro de /lancamentos, mesma
    // riqueza da coluna "Forma de Pagamento" da planilha real.
    @Test
    void listarPorCompetenciaComFormaPagamentoPropagaOFiltro() {
        LocalDate de = LocalDate.of(2026, 7, 1);
        LocalDate ate = LocalDate.of(2026, 7, 31);
        when(lancamentoRepository.buscarComFiltroPorCompetencia(null, null, null, null, FormaPagamentoLancamento.PIX, de, ate))
                .thenReturn(List.of());

        service().listar(de, ate, null, null, null, null, FormaPagamentoLancamento.PIX);

        verify(lancamentoRepository).buscarComFiltroPorCompetencia(null, null, null, null, FormaPagamentoLancamento.PIX, de, ate);
    }

    // M26 (absorvido de ContaPagarReceberServiceTest — "filtro mensal", change request 17/07/2026).
    @Test
    void listarPorVencimentoSemAnoMesNaoAplicaFiltroDePeriodo() {
        when(lancamentoRepository.buscarComFiltroPorVencimento(null, null, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM))
                .thenReturn(List.of());

        service().listarPorVencimento(null, null, null, null, null);

        verify(lancamentoRepository).buscarComFiltroPorVencimento(null, null, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM);
    }

    @Test
    void listarPorVencimentoComAnoEMesConvertePraJanelaDoMes() {
        when(lancamentoRepository.buscarComFiltroPorVencimento(TipoLancamento.DESPESA, null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1))).thenReturn(List.of());

        service().listarPorVencimento(TipoLancamento.DESPESA, null, 2026, 7, null);

        verify(lancamentoRepository).buscarComFiltroPorVencimento(TipoLancamento.DESPESA, null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));
    }

    @Test
    void listarPorVencimentoComSoAnoOuSoMesIgnoraFiltroDePeriodo() {
        when(lancamentoRepository.buscarComFiltroPorVencimento(null, null, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM))
                .thenReturn(List.of());

        service().listarPorVencimento(null, null, 2026, null, null);
        service().listarPorVencimento(null, null, null, 7, null);

        verify(lancamentoRepository, times(2))
                .buscarComFiltroPorVencimento(null, null, null, SEM_FILTRO_INICIO, SEM_FILTRO_FIM);
    }

    @Test
    void listarPorVencimentoComEventoIdPropagaPraORepositorio() {
        UUID eventoId = UUID.randomUUID();
        when(lancamentoRepository.buscarComFiltroPorVencimento(null, null, eventoId, SEM_FILTRO_INICIO, SEM_FILTRO_FIM))
                .thenReturn(List.of());

        service().listarPorVencimento(null, null, null, null, eventoId);

        verify(lancamentoRepository).buscarComFiltroPorVencimento(null, null, eventoId, SEM_FILTRO_INICIO, SEM_FILTRO_FIM);
    }

    @Test
    void liquidarAPagarViraRealizado() {
        UUID id = UUID.randomUUID();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.DESPESA, categoriaInfra(),
                "Servidor Hostinger", new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), StatusLancamento.PREVISTO,
                null, LocalDate.of(2026, 7, 10));
        when(lancamentoRepository.buscarPorIdComEvento(id)).thenReturn(Optional.of(lancamento));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LancamentoFinanceiro liquidado = service().liquidar(id, new LiquidarLancamentoRequest(LocalDate.of(2026, 7, 9)));

        assertThat(liquidado.getStatus()).isEqualTo(StatusLancamento.REALIZADO);
        assertThat(liquidado.getDataPagamento()).isEqualTo(LocalDate.of(2026, 7, 9));
        assertThat(liquidado.getDataCompetencia()).isEqualTo(LocalDate.of(2026, 7, 9));
        assertThat(liquidado.getValorPago()).isEqualByComparingTo("180.00");
    }

    @Test
    void liquidarLancamentoJaLiquidadoLancaErro() {
        UUID id = UUID.randomUUID();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.DESPESA, categoriaInfra(),
                "Servidor", new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), StatusLancamento.PREVISTO,
                null, LocalDate.of(2026, 7, 10));
        lancamento.liquidar(LocalDate.of(2026, 7, 9));
        when(lancamentoRepository.buscarPorIdComEvento(id)).thenReturn(Optional.of(lancamento));

        assertThatThrownBy(() -> service().liquidar(id, new LiquidarLancamentoRequest(LocalDate.now())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void liquidarLancamentoNaoEncontradoLancaErro() {
        UUID id = UUID.randomUUID();
        when(lancamentoRepository.buscarPorIdComEvento(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().liquidar(id, new LiquidarLancamentoRequest(LocalDate.now())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Gap 1 (raio-x, 18/07/2026) — StatusLancamento.PARCIAL.
    @Test
    void liquidarParcialAbaixoDoValorTotalFicaParcial() {
        UUID id = UUID.randomUUID();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, categoriaAssinatura(),
                "Mensalidade João Silva", new BigDecimal("1000.00"), LocalDate.of(2026, 8, 5), StatusLancamento.PREVISTO,
                null, LocalDate.of(2026, 8, 5));
        when(lancamentoRepository.buscarPorIdComEvento(id)).thenReturn(Optional.of(lancamento));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LiquidarParcialLancamentoRequest(new BigDecimal("400.00"), LocalDate.of(2026, 7, 19));
        LancamentoFinanceiro parcial = service().liquidarParcial(id, request);

        assertThat(parcial.getStatus()).isEqualTo(StatusLancamento.PARCIAL);
        assertThat(parcial.getValorPago()).isEqualByComparingTo("400.00");
    }

    @Test
    void liquidarParcialAcumulaAteCobrirOValorTotalEViraRealizado() {
        UUID id = UUID.randomUUID();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.DESPESA, categoriaInfra(),
                "Servidor Hostinger", new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), StatusLancamento.PREVISTO,
                null, LocalDate.of(2026, 7, 10));
        when(lancamentoRepository.buscarPorIdComEvento(id)).thenReturn(Optional.of(lancamento));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().liquidarParcial(id, new LiquidarParcialLancamentoRequest(new BigDecimal("100.00"), LocalDate.of(2026, 7, 15)));
        LancamentoFinanceiro liquidado = service().liquidarParcial(id,
                new LiquidarParcialLancamentoRequest(new BigDecimal("80.00"), LocalDate.of(2026, 7, 20)));

        assertThat(liquidado.getStatus()).isEqualTo(StatusLancamento.REALIZADO);
        assertThat(liquidado.getValorPago()).isEqualByComparingTo("180.00");
    }

    @Test
    void liquidarParcialAcimaDoValorRestanteLancaErro() {
        UUID id = UUID.randomUUID();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.DESPESA, categoriaInfra(),
                "Servidor", new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), StatusLancamento.PREVISTO,
                null, LocalDate.of(2026, 7, 10));
        when(lancamentoRepository.buscarPorIdComEvento(id)).thenReturn(Optional.of(lancamento));

        assertThatThrownBy(() -> service().liquidarParcial(id,
                new LiquidarParcialLancamentoRequest(new BigDecimal("200.00"), LocalDate.now())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void liquidarParcialDeLancamentoJaLiquidadoLancaErro() {
        UUID id = UUID.randomUUID();
        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.DESPESA, categoriaInfra(),
                "Servidor", new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), StatusLancamento.PREVISTO,
                null, LocalDate.of(2026, 7, 10));
        lancamento.liquidar(LocalDate.of(2026, 7, 9));
        when(lancamentoRepository.buscarPorIdComEvento(id)).thenReturn(Optional.of(lancamento));

        assertThatThrownBy(() -> service().liquidarParcial(id,
                new LiquidarParcialLancamentoRequest(new BigDecimal("10.00"), LocalDate.now())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void argumentCaptorAindaFuncionaComOConstrutorCanonico() {
        UUID categoriaId = UUID.randomUUID();
        CategoriaFinanceira categoria = categoriaInfra();
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarLancamentoRequest(TipoLancamento.DESPESA, categoriaId, "Servidor",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), StatusLancamento.PREVISTO);
        service().criar(request);

        ArgumentCaptor<LancamentoFinanceiro> captor = ArgumentCaptor.forClass(LancamentoFinanceiro.class);
        verify(lancamentoRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoria()).isEqualTo(categoria);
    }
}
