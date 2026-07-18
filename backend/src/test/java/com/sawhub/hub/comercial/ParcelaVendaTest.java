package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.financeiro.ContaPagarReceber;
import com.sawhub.hub.financeiro.TipoConta;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** RED primeiro: ParcelaVenda ainda não existe. M25 — parcelamento estruturado, cada parcela
 * vira um ContaPagarReceber A_RECEBER quando criada (ver LeadService.fecharVenda). */
class ParcelaVendaTest {

    @Test
    void nasceSemContaVinculadaEDepoisPodeVincular() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null, null);
        ParcelaVenda parcela = new ParcelaVenda(lead, 1, new BigDecimal("2000.00"), LocalDate.of(2026, 8, 17));

        assertThat(parcela.getContaPagarReceber()).isNull();
        assertThat(parcela.getNumero()).isEqualTo(1);
        assertThat(parcela.getValor()).isEqualByComparingTo("2000.00");

        ContaPagarReceber conta = new ContaPagarReceber(TipoConta.A_RECEBER, "Parcela 1", new BigDecimal("2000.00"),
                LocalDate.of(2026, 8, 17), null);
        parcela.vincularConta(conta);

        assertThat(parcela.getContaPagarReceber()).isSameAs(conta);
    }
}
