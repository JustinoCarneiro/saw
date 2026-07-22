package com.sawhub.hub.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sawhub.hub.financeiro.dto.CriarTransferenciaRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** "Transferências Entre Contas" (change request pós-MVP, E14, reunião 17/07/2026) — não é
 * receita nem despesa, puramente informativo. */
@ExtendWith(MockitoExtension.class)
class TransferenciaBancariaServiceTest {

    @Mock
    private ContaBancariaRepository contaBancariaRepository;
    @Mock
    private TransferenciaBancariaRepository transferenciaRepository;

    private TransferenciaBancariaService service() {
        return new TransferenciaBancariaService(contaBancariaRepository, transferenciaRepository);
    }

    private static ContaBancaria conta(String nome) {
        ContaBancaria conta = new ContaBancaria(nome);
        ReflectionTestUtils.setField(conta, "id", UUID.randomUUID());
        return conta;
    }

    @Test
    void registrarComMesmaContaDeOrigemEDestinoLancaErro() {
        UUID contaId = UUID.randomUUID();
        var request = new CriarTransferenciaRequest(contaId, contaId, new BigDecimal("500"), LocalDate.of(2026, 7, 15), null);

        assertThatThrownBy(() -> service().registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mesma");
        verifyNoInteractions(contaBancariaRepository, transferenciaRepository);
    }

    @Test
    void registrarComContaDeOrigemInexistenteLancaErro() {
        ContaBancaria destino = conta("Infinity Pay");
        UUID origemId = UUID.randomUUID();
        when(contaBancariaRepository.findById(origemId)).thenReturn(Optional.empty());

        var request = new CriarTransferenciaRequest(origemId, destino.getId(), new BigDecimal("500"), LocalDate.of(2026, 7, 15), null);

        assertThatThrownBy(() -> service().registrar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origem");
    }

    @Test
    void registrarComContasValidasCriaTransferencia() {
        ContaBancaria itau = conta("Itaú");
        ContaBancaria infinity = conta("Infinity Pay");
        when(contaBancariaRepository.findById(itau.getId())).thenReturn(Optional.of(itau));
        when(contaBancariaRepository.findById(infinity.getId())).thenReturn(Optional.of(infinity));
        when(transferenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarTransferenciaRequest(itau.getId(), infinity.getId(), new BigDecimal("500"),
                LocalDate.of(2026, 7, 15), "Empréstimo interno");
        TransferenciaBancaria transferencia = service().registrar(request);

        assertThat(transferencia.getContaOrigem()).isSameAs(itau);
        assertThat(transferencia.getContaDestino()).isSameAs(infinity);
        assertThat(transferencia.getValor()).isEqualByComparingTo("500");
    }
}
