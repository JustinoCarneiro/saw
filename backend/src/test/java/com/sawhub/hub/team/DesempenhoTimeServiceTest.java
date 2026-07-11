package com.sawhub.hub.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sawhub.hub.comercial.LeadRepository;
import com.sawhub.hub.comercial.MetaComercial;
import com.sawhub.hub.comercial.MetaComercialRepository;
import com.sawhub.hub.comercial.StatusLead;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentoria.Mentoria;
import com.sawhub.hub.mentoria.MentoriaRepository;
import com.sawhub.hub.mentoria.StatusMentoria;
import com.sawhub.hub.mentoria.TipoMentoria;
import com.sawhub.hub.team.dto.DesempenhoColaboradorResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H15.7 — RED primeiro: DesempenhoTimeService ainda não existia antes deste ciclo (M20). */
@ExtendWith(MockitoExtension.class)
class DesempenhoTimeServiceTest {

    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private MetaComercialRepository metaComercialRepository;
    @Mock
    private LeadRepository leadRepository;

    private DesempenhoTimeService service() {
        return new DesempenhoTimeService(colaboradorRepository, mentoriaRepository, metaComercialRepository,
                leadRepository);
    }

    private static Colaborador colaborador(UUID id, String nome, Area area) {
        Colaborador c = new Colaborador(null, nome, area);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static Mentorado mentorado() {
        Mentorado m = new Mentorado(null, "Mentorado", "Negócio", Plano.BASICO, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
        return m;
    }

    private static Mentoria mentoria(Colaborador mentor, StatusMentoria status, Instant dataHora) {
        Mentoria m = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, java.util.Set.of(mentorado()), dataHora, 60, null,
                null);
        ReflectionTestUtils.setField(m, "status", status);
        return m;
    }

    @Test
    void contaSoMentoriasRealizadasDentroDoPeriodo() {
        UUID mentorId = UUID.randomUUID();
        Colaborador mentor = colaborador(mentorId, "Lucas", Area.GESTAO_PERFORMANCE);
        when(colaboradorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(mentor));
        when(metaComercialRepository.buscarComVendedorPorPeriodo(2026, 7)).thenReturn(List.of());

        Instant dentroDoPeriodo = Instant.parse("2026-07-15T12:00:00Z");
        Instant foraDoPeriodo = Instant.parse("2026-08-01T12:00:00Z");
        when(mentoriaRepository.buscarPorMentor(mentor)).thenReturn(List.of(
                mentoria(mentor, StatusMentoria.REALIZADA, dentroDoPeriodo),
                mentoria(mentor, StatusMentoria.AGENDADA, dentroDoPeriodo),
                mentoria(mentor, StatusMentoria.REALIZADA, foraDoPeriodo)));

        List<DesempenhoColaboradorResponse> resultado = service().desempenho(2026, 7);

        assertThat(resultado).hasSize(1);
        DesempenhoColaboradorResponse item = resultado.get(0);
        assertThat(item.mentoriasRealizadas()).isEqualTo(1L);
        assertThat(item.metaFechamentos()).isNull();
        assertThat(item.fechamentosRealizados()).isNull();
        assertThat(item.pctAtingidoFechamentos()).isNull();
    }

    @Test
    void colaboradorComMetaComercialTrazMetaXRealizado() {
        UUID vendedorId = UUID.randomUUID();
        Colaborador vendedor = colaborador(vendedorId, "Paula", Area.COMERCIAL);
        when(colaboradorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(vendedor));
        when(mentoriaRepository.buscarPorMentor(vendedor)).thenReturn(List.of());

        MetaComercial meta = new MetaComercial(vendedor, 2026, 7, 6);
        when(metaComercialRepository.buscarComVendedorPorPeriodo(2026, 7)).thenReturn(List.of(meta));
        when(leadRepository.countByVendedorIdAndStatusAndDataFechamentoBetween(
                eq(vendedorId), eq(StatusLead.FECHADO), any(Instant.class), any(Instant.class))).thenReturn(4L);

        List<DesempenhoColaboradorResponse> resultado = service().desempenho(2026, 7);

        assertThat(resultado).hasSize(1);
        DesempenhoColaboradorResponse item = resultado.get(0);
        assertThat(item.mentoriasRealizadas()).isEqualTo(0L);
        assertThat(item.metaFechamentos()).isEqualTo(6);
        assertThat(item.fechamentosRealizados()).isEqualTo(4L);
        assertThat(item.pctAtingidoFechamentos()).isCloseTo(66.7, within(0.1));
    }

    @Test
    void limiteSuperiorDoPeriodoEExclusivo() {
        UUID mentorId = UUID.randomUUID();
        Colaborador mentor = colaborador(mentorId, "Lucas", Area.GESTAO_PERFORMANCE);
        when(colaboradorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(mentor));
        when(metaComercialRepository.buscarComVendedorPorPeriodo(2026, 7)).thenReturn(List.of());

        Instant inicioAgosto = Instant.parse("2026-08-01T00:00:00Z");
        when(mentoriaRepository.buscarPorMentor(mentor)).thenReturn(List.of(
                mentoria(mentor, StatusMentoria.REALIZADA, inicioAgosto),
                mentoria(mentor, StatusMentoria.REALIZADA, inicioAgosto.minus(1, ChronoUnit.MILLIS))));

        List<DesempenhoColaboradorResponse> resultado = service().desempenho(2026, 7);

        assertThat(resultado.get(0).mentoriasRealizadas()).isEqualTo(1L);
    }
}
