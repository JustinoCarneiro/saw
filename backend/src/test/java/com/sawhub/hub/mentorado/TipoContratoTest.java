package com.sawhub.hub.mentorado;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** RED primeiro: TipoContrato ainda não existe neste ponto do ciclo. Regra confirmada com o
 * cliente (reunião 17/07/2026, docs/reuniao-2026-07-17-atualizacoes.md): Mentoria Contínua e
 * Individual vencem 12 meses depois do fechamento; Consultoria é "esporádica", sem prazo fixo. */
class TipoContratoTest {

    @Test
    void mentoriaContinuaVenceDozeMesesDepoisDoFechamento() {
        LocalDate fechamento = LocalDate.of(2026, 7, 17);

        LocalDate vencimento = TipoContrato.MENTORIA_CONTINUA.calcularVencimento(fechamento);

        assertThat(vencimento).isEqualTo(LocalDate.of(2027, 7, 17));
    }

    @Test
    void mentoriaIndividualVenceDozeMesesDepoisDoFechamento() {
        LocalDate fechamento = LocalDate.of(2026, 1, 31);

        LocalDate vencimento = TipoContrato.MENTORIA_INDIVIDUAL.calcularVencimento(fechamento);

        assertThat(vencimento).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    @Test
    void consultoriaNaoTemVencimentoFixo() {
        LocalDate vencimento = TipoContrato.CONSULTORIA.calcularVencimento(LocalDate.of(2026, 7, 17));

        assertThat(vencimento).isNull();
    }
}
