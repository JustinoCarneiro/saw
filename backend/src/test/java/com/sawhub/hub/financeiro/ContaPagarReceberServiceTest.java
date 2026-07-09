package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sawhub.hub.financeiro.dto.CriarContaRequest;
import com.sawhub.hub.financeiro.dto.LiquidarContaRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** H14.4 — RED primeiro: ContaPagarReceberService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class ContaPagarReceberServiceTest {

    @Mock
    private ContaPagarReceberRepository contaRepository;
    @Mock
    private LancamentoFinanceiroRepository lancamentoRepository;
    @Mock
    private CategoriaFinanceiraRepository categoriaRepository;

    private ContaPagarReceberService service() {
        return new ContaPagarReceberService(contaRepository, lancamentoRepository, categoriaRepository);
    }

    @Test
    void criarPersisteContaPendente() {
        UUID categoriaId = UUID.randomUUID();
        CategoriaFinanceira categoria = new CategoriaFinanceira("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(contaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarContaRequest(TipoConta.A_PAGAR, "Servidor Hostinger — julho",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), categoriaId);

        ContaPagarReceber conta = service().criar(request);

        assertThat(conta.getStatus()).isEqualTo(StatusConta.PENDENTE);
        assertThat(conta.getTipo()).isEqualTo(TipoConta.A_PAGAR);
        assertThat(conta.getValor()).isEqualByComparingTo("180.00");
    }

    @Test
    void liquidarAPagarViraPagoEGeraLancamentoDeDespesa() {
        UUID contaId = UUID.randomUUID();
        CategoriaFinanceira categoria = new CategoriaFinanceira("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor Hostinger",
                new BigDecimal("180.00"), LocalDate.of(2026, 7, 10), categoria);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new LiquidarContaRequest(LocalDate.of(2026, 7, 9), true);
        ContaPagarReceber liquidada = service().liquidar(contaId, request);

        assertThat(liquidada.getStatus()).isEqualTo(StatusConta.PAGO);
        assertThat(liquidada.getDataPagamento()).isEqualTo(LocalDate.of(2026, 7, 9));

        ArgumentCaptor<LancamentoFinanceiro> captor = ArgumentCaptor.forClass(LancamentoFinanceiro.class);
        verify(lancamentoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoLancamento.DESPESA);
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusLancamento.REALIZADO);
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("180.00");
    }

    @Test
    void liquidarARecerberViraRecebido() {
        UUID contaId = UUID.randomUUID();
        CategoriaFinanceira categoria = new CategoriaFinanceira("Assinaturas", TipoLancamento.RECEITA, GrupoDre.RECEITA_BRUTA, OrigemReceita.ASSINATURA);
        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_RECEBER, "Mensalidade João Silva",
                new BigDecimal("397.00"), LocalDate.of(2026, 7, 5), categoria);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(lancamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContaPagarReceber liquidada = service().liquidar(contaId, new LiquidarContaRequest(LocalDate.of(2026, 7, 5), true));

        assertThat(liquidada.getStatus()).isEqualTo(StatusConta.RECEBIDO);
    }

    @Test
    void liquidarSemCriarLancamentoNaoTocaNoRepositorioDeLancamento() {
        UUID contaId = UUID.randomUUID();
        CategoriaFinanceira categoria = new CategoriaFinanceira("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor", new BigDecimal("180.00"),
                LocalDate.of(2026, 7, 10), categoria);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));
        when(contaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().liquidar(contaId, new LiquidarContaRequest(LocalDate.of(2026, 7, 9), false));

        verifyNoInteractions(lancamentoRepository);
    }

    @Test
    void liquidarContaSemCategoriaPedindoLancamentoLancaErro() {
        UUID contaId = UUID.randomUUID();
        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_PAGAR, "Sem categoria", new BigDecimal("50.00"),
                LocalDate.of(2026, 7, 10), null);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service().liquidar(contaId, new LiquidarContaRequest(LocalDate.now(), true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("categoria");
    }

    @Test
    void liquidarContaJaLiquidadaLancaErro() {
        UUID contaId = UUID.randomUUID();
        CategoriaFinanceira categoria = new CategoriaFinanceira("Infra", TipoLancamento.DESPESA, GrupoDre.CUSTOS, null);
        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_PAGAR, "Servidor", new BigDecimal("180.00"),
                LocalDate.of(2026, 7, 10), categoria);
        conta.liquidar(LocalDate.of(2026, 7, 9), null);
        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> service().liquidar(contaId, new LiquidarContaRequest(LocalDate.now(), false)))
                .isInstanceOf(IllegalStateException.class);
    }
}
