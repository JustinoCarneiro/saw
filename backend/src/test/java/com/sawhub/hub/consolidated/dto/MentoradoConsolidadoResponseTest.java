package com.sawhub.hub.consolidated.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.mentorado.NivelEngajamento;
import com.sawhub.hub.mentorado.RiscoChurn;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Regra de negócio central do E17 (Painel Consolidado): progresso ponderado por peso do
 * encaminhamento e os limiares de status (EM_DIA/ATENCAO/ATRASADO). Se isto quebrar, o
 * ranking e o resumo do painel mentem pro Fundador sobre quem está atrasado de verdade.
 */
class MentoradoConsolidadoResponseTest {

    private static MentoradoConsolidadoRow row(long pesoConcluido, long pesoTotal, int ferrConcluidas, int ferrTotal) {
        return new MentoradoConsolidadoRow(UUID.randomUUID(), "Fulano", "Negócio do Fulano",
                BigDecimal.TEN, ferrConcluidas, ferrTotal, 10L, 5L, pesoTotal, pesoConcluido, null, null);
    }

    @Test
    void progressoEArredondadoParaOInteiroMaisProximo() {
        // 6/9 = 66.666...% -> arredonda pra 67 (bate com o dado real seedado da Ana Costa)
        var response = MentoradoConsolidadoResponse.from(row(6, 9, 2, 3), null);
        assertThat(response.progressoPct()).isEqualTo(67);
    }

    @Test
    void semEncaminhamentoNenhum_progressoZeroENaoDividePorZero() {
        var response = MentoradoConsolidadoResponse.from(row(0, 0, 0, 0), null);
        assertThat(response.progressoPct()).isZero();
        assertThat(response.ferramentasPct()).isZero();
    }

    @Test
    void ferramentasPctSegueAMesmaLogicaDeArredondamento() {
        // 2/3 = 66.66...% -> 67
        var response = MentoradoConsolidadoResponse.from(row(1, 1, 2, 3), null);
        assertThat(response.ferramentasPct()).isEqualTo(67);
    }

    @Test
    void status_emDia_a_partir_de_60_por_cento_inclusive() {
        assertThat(MentoradoConsolidadoResponse.from(row(60, 100, 0, 1), null).status()).isEqualTo("EM_DIA");
        assertThat(MentoradoConsolidadoResponse.from(row(59, 100, 0, 1), null).status()).isEqualTo("ATENCAO");
    }

    @Test
    void status_atencao_a_partir_de_30_por_cento_inclusive() {
        assertThat(MentoradoConsolidadoResponse.from(row(30, 100, 0, 1), null).status()).isEqualTo("ATENCAO");
        assertThat(MentoradoConsolidadoResponse.from(row(29, 100, 0, 1), null).status()).isEqualTo("ATRASADO");
    }

    @Test
    void status_atrasado_abaixo_de_30_por_cento() {
        assertThat(MentoradoConsolidadoResponse.from(row(0, 100, 0, 1), null).status()).isEqualTo("ATRASADO");
    }

    @Test
    void status_100_por_cento_e_em_dia() {
        assertThat(MentoradoConsolidadoResponse.from(row(10, 10, 3, 3), null).status()).isEqualTo("EM_DIA");
    }

    @Test
    void mantemOsCamposCrusDaRowQueNaoSaoRecalculados() {
        UUID id = UUID.randomUUID();
        var linha = new MentoradoConsolidadoRow(id, "João Silva", "Restaurante Sabor & Arte",
                new BigDecimal("18.0"), 3, 3, 10L, 9L, 10L, 9L, null, null);

        var response = MentoradoConsolidadoResponse.from(linha, null);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.nome()).isEqualTo("João Silva");
        assertThat(response.negocio()).isEqualTo("Restaurante Sabor & Arte");
        assertThat(response.encaminhamentosCumpridos()).isEqualTo(9L);
        assertThat(response.encaminhamentosTotal()).isEqualTo(10L);
        assertThat(response.crescimentoFaturamentoPct()).isEqualByComparingTo("18.0");
    }

    // E17/M27 — frequenciaMentoriaPct/nivelEngajamento/riscoChurn são aditivos: repassados sem
    // recálculo (frequência vem de fora, os dois eixos vêm direto da row).
    @Test
    void repassaFrequenciaNivelEngajamentoERiscoChurnSemRecalculo() {
        UUID id = UUID.randomUUID();
        var linha = new MentoradoConsolidadoRow(id, "Fulano", "Negócio", BigDecimal.TEN, 0, 0, 0L, 0L, 1L, 1L,
                NivelEngajamento.ALTO, RiscoChurn.ATENCAO);

        var response = MentoradoConsolidadoResponse.from(linha, 80);

        assertThat(response.frequenciaMentoriaPct()).isEqualTo(80);
        assertThat(response.nivelEngajamento()).isEqualTo(NivelEngajamento.ALTO);
        assertThat(response.riscoChurn()).isEqualTo(RiscoChurn.ATENCAO);
    }

    @Test
    void frequenciaMentoriaPctNuloQuandoMentoradoNuncaParticipouDeMentoriaEmGrupo() {
        var response = MentoradoConsolidadoResponse.from(row(1, 1, 0, 0), null);
        assertThat(response.frequenciaMentoriaPct()).isNull();
    }
}
