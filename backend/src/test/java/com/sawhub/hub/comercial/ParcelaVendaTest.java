package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.financeiro.LancamentoFinanceiro;
import com.sawhub.hub.financeiro.StatusLancamento;
import com.sawhub.hub.financeiro.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** M25 — parcelamento estruturado, cada parcela vira um LancamentoFinanceiro RECEITA quando
 * criada (ver LeadService.fecharVenda). M26 repontou de ContaPagarReceber pra
 * LancamentoFinanceiro (merge de entidade, ver ROADMAP.md § "Blueprint (M26)"). */
class ParcelaVendaTest {

    @Test
    void nasceSemLancamentoVinculadoEDepoisPodeVincular() {
        Lead lead = new Lead("Maria Souza", "maria@restaurante.com", null, null);
        ParcelaVenda parcela = new ParcelaVenda(lead, 1, new BigDecimal("2000.00"), LocalDate.of(2026, 8, 17));

        assertThat(parcela.getLancamento()).isNull();
        assertThat(parcela.getNumero()).isEqualTo(1);
        assertThat(parcela.getValor()).isEqualByComparingTo("2000.00");

        LancamentoFinanceiro lancamento = new LancamentoFinanceiro(TipoLancamento.RECEITA, null, "Parcela 1",
                new BigDecimal("2000.00"), LocalDate.of(2026, 8, 17), StatusLancamento.PREVISTO, null, null,
                LocalDate.of(2026, 8, 17));
        parcela.vincularLancamento(lancamento);

        assertThat(parcela.getLancamento()).isSameAs(lancamento);
    }
}
