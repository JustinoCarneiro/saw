package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.financeiro.dto.RegistrarPosicaoCaixaRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** "Caixa do mês: Inicial, saldo por banco, Final" (change request pós-MVP, reunião
 * 17/07/2026). */
@ExtendWith(MockitoExtension.class)
class CaixaMensalServiceTest {

    @Mock
    private ContaBancariaRepository contaBancariaRepository;
    @Mock
    private PosicaoCaixaMensalRepository posicaoRepository;

    private CaixaMensalService service() {
        return new CaixaMensalService(contaBancariaRepository, posicaoRepository);
    }

    private static ContaBancaria conta(String nome) {
        ContaBancaria conta = new ContaBancaria(nome);
        ReflectionTestUtils.setField(conta, "id", UUID.randomUUID());
        return conta;
    }

    @Test
    void registrarPosicaoDeContaInexistenteLancaErro() {
        UUID contaId = UUID.randomUUID();
        when(contaBancariaRepository.findById(contaId)).thenReturn(Optional.empty());

        var request = new RegistrarPosicaoCaixaRequest(contaId, 2026, 7, new BigDecimal("1000"), new BigDecimal("1500"));

        assertThatThrownBy(() -> service().registrarPosicao(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");
    }

    @Test
    void registrarPosicaoNovaCriaRegistro() {
        ContaBancaria itau = conta("Itaú");
        when(contaBancariaRepository.findById(itau.getId())).thenReturn(Optional.of(itau));
        when(posicaoRepository.findByContaBancariaIdAndAnoAndMes(itau.getId(), 2026, 7)).thenReturn(Optional.empty());
        when(posicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new RegistrarPosicaoCaixaRequest(itau.getId(), 2026, 7, new BigDecimal("1000"), new BigDecimal("1500"));
        PosicaoCaixaMensal posicao = service().registrarPosicao(request);

        assertThat(posicao.getSaldoInicial()).isEqualByComparingTo("1000");
        assertThat(posicao.getSaldoFinal()).isEqualByComparingTo("1500");
    }

    // Upsert — registrar de novo o mesmo (conta, ano, mês) corrige o valor já digitado, não
    // duplica linha (mesma natureza de entrada manual da planilha real).
    @Test
    void registrarPosicaoExistenteAtualizaEmVezDeDuplicar() {
        ContaBancaria itau = conta("Itaú");
        PosicaoCaixaMensal existente = new PosicaoCaixaMensal(itau, 2026, 7, new BigDecimal("1000"), new BigDecimal("1500"));
        when(contaBancariaRepository.findById(itau.getId())).thenReturn(Optional.of(itau));
        when(posicaoRepository.findByContaBancariaIdAndAnoAndMes(itau.getId(), 2026, 7)).thenReturn(Optional.of(existente));
        when(posicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new RegistrarPosicaoCaixaRequest(itau.getId(), 2026, 7, new BigDecimal("1000"), new BigDecimal("1800"));
        PosicaoCaixaMensal posicao = service().registrarPosicao(request);

        assertThat(posicao).isSameAs(existente);
        assertThat(posicao.getSaldoFinal()).isEqualByComparingTo("1800");
    }

    @Test
    void caixaDoMesSomaTotalDeTodasAsContas() {
        ContaBancaria itau = conta("Itaú");
        ContaBancaria infinity = conta("Infinity Pay");
        when(posicaoRepository.buscarPorAnoMesComConta(2026, 7)).thenReturn(List.of(
                new PosicaoCaixaMensal(itau, 2026, 7, new BigDecimal("1000"), new BigDecimal("1200")),
                new PosicaoCaixaMensal(infinity, 2026, 7, new BigDecimal("500"), new BigDecimal("300"))));

        var caixa = service().caixaDoMes(2026, 7);

        assertThat(caixa.contas()).hasSize(2);
        assertThat(caixa.totalInicial()).isEqualByComparingTo("1500");
        assertThat(caixa.totalFinal()).isEqualByComparingTo("1500");
    }
}
