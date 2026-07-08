package com.sawhub.hub.comercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.dto.RankingItem;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H13.3 — RED primeiro: RankingComercialService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class RankingComercialServiceTest {

    @Mock
    private MetaComercialRepository metaComercialRepository;
    @Mock
    private LeadRepository leadRepository;

    private RankingComercialService service() {
        return new RankingComercialService(metaComercialRepository, leadRepository);
    }

    private static Colaborador colaborador(UUID id, String nome) {
        Colaborador c = new Colaborador(null, nome, Area.COMERCIAL, 0, BigDecimal.ZERO);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void rankingCalculaRealizadoEPercentualAtingido() {
        UUID paulaId = UUID.randomUUID();
        MetaComercial meta = new MetaComercial(colaborador(paulaId, "Paula"), 2026, 7, 6);
        when(metaComercialRepository.buscarComVendedorPorPeriodo(2026, 7)).thenReturn(List.of(meta));
        when(leadRepository.countByVendedorIdAndStatusAndDataFechamentoBetween(
                eq(paulaId), eq(StatusLead.FECHADO), any(Instant.class), any(Instant.class))).thenReturn(4L);

        List<RankingItem> ranking = service().ranking(2026, 7);

        assertThat(ranking).hasSize(1);
        RankingItem item = ranking.get(0);
        assertThat(item.vendedor().nome()).isEqualTo("Paula");
        assertThat(item.metaFechamentos()).isEqualTo(6);
        assertThat(item.realizado()).isEqualTo(4L);
        assertThat(item.pctAtingido()).isCloseTo(66.7, within(0.1));
    }

    @Test
    void rankingOrdenaPorRealizadoDecrescente() {
        UUID paulaId = UUID.randomUUID();
        UUID ricardoId = UUID.randomUUID();
        MetaComercial metaPaula = new MetaComercial(colaborador(paulaId, "Paula"), 2026, 7, 6);
        MetaComercial metaRicardo = new MetaComercial(colaborador(ricardoId, "Ricardo"), 2026, 7, 6);
        when(metaComercialRepository.buscarComVendedorPorPeriodo(2026, 7)).thenReturn(List.of(metaPaula, metaRicardo));
        when(leadRepository.countByVendedorIdAndStatusAndDataFechamentoBetween(
                eq(paulaId), eq(StatusLead.FECHADO), any(Instant.class), any(Instant.class))).thenReturn(2L);
        when(leadRepository.countByVendedorIdAndStatusAndDataFechamentoBetween(
                eq(ricardoId), eq(StatusLead.FECHADO), any(Instant.class), any(Instant.class))).thenReturn(5L);

        List<RankingItem> ranking = service().ranking(2026, 7);

        assertThat(ranking).extracting(r -> r.vendedor().nome()).containsExactly("Ricardo", "Paula");
    }

    @Test
    void rankingComMetaZeroNaoDivideParZero() {
        UUID paulaId = UUID.randomUUID();
        MetaComercial meta = new MetaComercial(colaborador(paulaId, "Paula"), 2026, 7, 0);
        when(metaComercialRepository.buscarComVendedorPorPeriodo(2026, 7)).thenReturn(List.of(meta));
        when(leadRepository.countByVendedorIdAndStatusAndDataFechamentoBetween(
                eq(paulaId), eq(StatusLead.FECHADO), any(Instant.class), any(Instant.class))).thenReturn(0L);

        List<RankingItem> ranking = service().ranking(2026, 7);

        assertThat(ranking.get(0).pctAtingido()).isZero();
    }
}
