package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentoria.dto.CriarMentoriaRequest;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H11.2 — RED primeiro: MentoriaService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class MentoriaServiceTest {

    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private MentoriaService service() {
        return new MentoriaService(mentoriaRepository, colaboradorRepository, mentoradoRepository);
    }

    private static Colaborador mentor(UUID id) {
        Colaborador c = new Colaborador(null, "Lucas", Area.GESTAO_PERFORMANCE, 10, BigDecimal.TEN);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static Mentorado mentorado(UUID id, String nome) {
        Mentorado m = new Mentorado(null, nome, null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    @Test
    void listarDelegaStatusParaOBancoEFiltraDataEmMemoria() {
        // Bug ao vivo (verificação de M06 via curl): um filtro opcional de Instant nulo em JPQL
        // ("? IS NULL OR ...") faz o Postgres falhar ao inferir o tipo do parâmetro — de/ate são
        // filtrados em memória (ver nota em MentoriaRepository.buscarPorStatus), não em SQL.
        Colaborador mentor = mentor(UUID.randomUUID());
        Mentoria dentro = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")),
                Instant.parse("2026-07-15T12:00:00Z"), 60, null, null);
        Mentoria fora = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, java.util.Set.of(mentorado(UUID.randomUUID(), "João")),
                Instant.parse("2026-08-01T12:00:00Z"), 60, null, null);
        when(mentoriaRepository.buscarPorStatus(StatusMentoria.AGENDADA)).thenReturn(List.of(dentro, fora));

        List<Mentoria> resultado = service().listar(StatusMentoria.AGENDADA,
                Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-31T23:59:59Z"));

        assertThat(resultado).containsExactly(dentro);
    }

    @Test
    void criarMentoriaIndividualComUmMentorado() {
        UUID mentorId = UUID.randomUUID();
        UUID mentoradoId = UUID.randomUUID();
        when(colaboradorRepository.findById(mentorId)).thenReturn(Optional.of(mentor(mentorId)));
        when(mentoradoRepository.findAllById(List.of(mentoradoId))).thenReturn(List.of(mentorado(mentoradoId, "Maria")));
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarMentoriaRequest(TipoMentoria.INDIVIDUAL, List.of(mentoradoId), mentorId,
                Instant.parse("2026-08-01T14:00:00Z"), 60, "https://meet.google.com/x", null);

        Mentoria mentoria = service().criar(request);

        assertThat(mentoria.getStatus()).isEqualTo(StatusMentoria.AGENDADA);
        assertThat(mentoria.getMentorados()).hasSize(1);
    }

    @Test
    void criarMentoriaIndividualComMaisDeUmMentoradoLancaErro() {
        UUID mentorId = UUID.randomUUID();
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();

        var request = new CriarMentoriaRequest(TipoMentoria.INDIVIDUAL, List.of(m1, m2), mentorId,
                Instant.parse("2026-08-01T14:00:00Z"), 60, null, null);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("individual");
    }

    @Test
    void criarMentoriaGrupoComVariosMentorados() {
        UUID mentorId = UUID.randomUUID();
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        when(colaboradorRepository.findById(mentorId)).thenReturn(Optional.of(mentor(mentorId)));
        when(mentoradoRepository.findAllById(List.of(m1, m2)))
                .thenReturn(List.of(mentorado(m1, "Maria"), mentorado(m2, "João")));
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarMentoriaRequest(TipoMentoria.GRUPO, List.of(m1, m2), mentorId,
                Instant.parse("2026-08-01T14:00:00Z"), 90, "https://meet.google.com/y", null);

        Mentoria mentoria = service().criar(request);

        assertThat(mentoria.getMentorados()).hasSize(2);
    }

    @Test
    void criarComMentoradoInexistenteLancaErro() {
        UUID mentorId = UUID.randomUUID();
        UUID mentoradoId = UUID.randomUUID();
        when(colaboradorRepository.findById(mentorId)).thenReturn(Optional.of(mentor(mentorId)));
        when(mentoradoRepository.findAllById(List.of(mentoradoId))).thenReturn(List.of());

        var request = new CriarMentoriaRequest(TipoMentoria.INDIVIDUAL, List.of(mentoradoId), mentorId,
                Instant.parse("2026-08-01T14:00:00Z"), 60, null, null);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não foram encontrados");
    }

    @Test
    void confirmarAPartirDeAgendada() {
        UUID id = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")), Instant.now(), 60, null, null);
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mentoria confirmada = service().avancarStatus(id, StatusMentoria.CONFIRMADA);

        assertThat(confirmada.getStatus()).isEqualTo(StatusMentoria.CONFIRMADA);
    }

    @Test
    void avancarParaRealizadaLancaErroApontandoEndpointDedicado() {
        UUID id = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")), Instant.now(), 60, null, null);
        mentoria.confirmar();
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));

        assertThatThrownBy(() -> service().avancarStatus(id, StatusMentoria.REALIZADA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("realizar");
    }

    @Test
    void cancelarAPartirDeConfirmada() {
        UUID id = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")), Instant.now(), 60, null, null);
        mentoria.confirmar();
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mentoria cancelada = service().avancarStatus(id, StatusMentoria.CANCELADA);

        assertThat(cancelada.getStatus()).isEqualTo(StatusMentoria.CANCELADA);
    }
}
