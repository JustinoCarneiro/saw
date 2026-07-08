package com.sawhub.hub.consolidated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sawhub.hub.consolidated.dto.MentoradoConsolidadoRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsolidatedServiceTest {

    @Mock
    private ConsolidatedRepository consolidatedRepository;

    private ConsolidatedService service() {
        return new ConsolidatedService(consolidatedRepository);
    }

    private static MentoradoConsolidadoRow row(String nome, String cresc, long pesoConcluido, long pesoTotal) {
        return new MentoradoConsolidadoRow(java.util.UUID.randomUUID(), nome, "negócio",
                new BigDecimal(cresc), 0, 0, 0L, 0L, pesoTotal, pesoConcluido);
    }

    @Test
    void listarMentoradosOrdenaPorNomeAlfabetico() {
        when(consolidatedRepository.buscarConsolidado()).thenReturn(List.of(
                row("Rafael Gomes", "1.0", 1, 10),
                row("Ana Costa", "1.0", 1, 10),
                row("João Silva", "1.0", 1, 10)));

        var nomes = service().listarMentorados().stream().map(m -> m.nome()).toList();

        assertThat(nomes).containsExactly("Ana Costa", "João Silva", "Rafael Gomes");
    }

    @Test
    void resumoContaCadaStatusCorretamenteEMediaOProgresso() {
        when(consolidatedRepository.buscarConsolidado()).thenReturn(List.of(
                row("A", "1", 100, 100), // 100% -> EM_DIA
                row("B", "1", 40, 100),  // 40%  -> ATENCAO
                row("C", "1", 10, 100),  // 10%  -> ATRASADO
                row("D", "1", 60, 100)   // 60%  -> EM_DIA
        ));

        var resumo = service().resumo();

        assertThat(resumo.total()).isEqualTo(4);
        assertThat(resumo.emDia()).isEqualTo(2);
        assertThat(resumo.atencao()).isEqualTo(1);
        assertThat(resumo.atrasado()).isEqualTo(1);
        // média de 100, 40, 10, 60 = 52.5 -> arredonda pra 53 (RoundingMode padrão do Math.round)
        assertThat(resumo.progressoMedioPct()).isEqualTo(53);
    }

    @Test
    void resumoComListaVaziaNaoQuebraEZeraTudo() {
        when(consolidatedRepository.buscarConsolidado()).thenReturn(List.of());

        var resumo = service().resumo();

        assertThat(resumo.total()).isZero();
        assertThat(resumo.progressoMedioPct()).isZero();
    }

    @Test
    void rankingOrdenaPorCrescimentoDescendenteELimitaAoTopN() {
        when(consolidatedRepository.buscarConsolidado()).thenReturn(List.of(
                row("Fernanda Lima", "24.0", 1, 1),
                row("Carlos Menezes", "5.0", 1, 1),
                row("João Silva", "18.0", 1, 1),
                row("Marina Souza", "-8.0", 1, 1)));

        var ranking = service().rankingFaturamento(3);

        assertThat(ranking).hasSize(3);
        assertThat(ranking.get(0).nome()).isEqualTo("Fernanda Lima");
        assertThat(ranking.get(0).pos()).isEqualTo(1);
        assertThat(ranking.get(1).nome()).isEqualTo("João Silva");
        assertThat(ranking.get(1).pos()).isEqualTo(2);
        assertThat(ranking.get(2).nome()).isEqualTo("Carlos Menezes");
        assertThat(ranking.get(2).pos()).isEqualTo(3);
    }

    @Test
    void rankingTrataCrescimentoNuloComoZero() {
        var linhaComNulo = new MentoradoConsolidadoRow(java.util.UUID.randomUUID(), "Sem Dado", "negócio",
                null, 0, 0, 0L, 0L, 1L, 1L);
        when(consolidatedRepository.buscarConsolidado()).thenReturn(List.of(linhaComNulo));

        var ranking = service().rankingFaturamento(3);

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).crescimentoFaturamentoPct()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
