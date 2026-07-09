package com.sawhub.hub.meta.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sawhub.hub.meta.Meta;
import com.sawhub.hub.meta.StatusMeta;
import com.sawhub.hub.meta.SubStatusMeta;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** H3.1 — regra central do E3: sub-status derivado de prazo vs hoje, só existe enquanto ATIVA.
 * Se isto quebrar, o mentorado vê "No prazo" numa meta atrasada de verdade. */
class MetaResponseTest {

    private static Meta meta(LocalDate prazo) {
        return new Meta(null, "Reduzir CMV", "desc", prazo);
    }

    @Test
    void ativaComPrazoFuturoLongeENoPrazo() {
        var response = MetaResponse.from(meta(LocalDate.of(2026, 8, 1)), LocalDate.of(2026, 7, 1));
        assertThat(response.diasRestantes()).isEqualTo(31);
        assertThat(response.subStatus()).isEqualTo(SubStatusMeta.NO_PRAZO);
    }

    @Test
    void ativaComPrazoA7DiasOuMenosEAtencaoInclusive() {
        assertThat(MetaResponse.from(meta(LocalDate.of(2026, 7, 8)), LocalDate.of(2026, 7, 1)).subStatus())
                .isEqualTo(SubStatusMeta.ATENCAO);
        assertThat(MetaResponse.from(meta(LocalDate.of(2026, 7, 9)), LocalDate.of(2026, 7, 1)).subStatus())
                .isEqualTo(SubStatusMeta.NO_PRAZO);
    }

    @Test
    void ativaComPrazoJaPassadoEAtrasada() {
        var response = MetaResponse.from(meta(LocalDate.of(2026, 6, 30)), LocalDate.of(2026, 7, 1));
        assertThat(response.diasRestantes()).isEqualTo(-1);
        assertThat(response.subStatus()).isEqualTo(SubStatusMeta.ATRASADA);
    }

    @Test
    void prazoHojeEAtencaoNaoAtrasada() {
        assertThat(MetaResponse.from(meta(LocalDate.of(2026, 7, 1)), LocalDate.of(2026, 7, 1)).subStatus())
                .isEqualTo(SubStatusMeta.ATENCAO);
    }

    @Test
    void concluidaNuncaTemSubStatusMesmoComPrazoPassado() {
        Meta m = meta(LocalDate.of(2026, 6, 1));
        ReflectionTestUtils.setField(m, "status", StatusMeta.CONCLUIDA);
        assertThat(MetaResponse.from(m, LocalDate.of(2026, 7, 1)).subStatus()).isNull();
    }

    @Test
    void pausadaNuncaTemSubStatus() {
        Meta m = meta(LocalDate.of(2026, 8, 1));
        ReflectionTestUtils.setField(m, "status", StatusMeta.PAUSADA);
        assertThat(MetaResponse.from(m, LocalDate.of(2026, 7, 1)).subStatus()).isNull();
    }
}
