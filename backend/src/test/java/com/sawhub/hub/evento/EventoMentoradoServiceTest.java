package com.sawhub.hub.evento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.evento.dto.EventoMentoradoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H7.1-H7.2 — RED primeiro: EventoMentoradoService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class EventoMentoradoServiceTest {

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private InscricaoEventoRepository inscricaoEventoRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private EventoMentoradoService service() {
        return new EventoMentoradoService(eventoRepository, inscricaoEventoRepository, mentoradoRepository);
    }

    private static Mentorado mentorado(UUID id) {
        Mentorado m = new Mentorado(null, "Maria", null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Evento evento(UUID id, Integer vagas) {
        Evento e = new Evento("Encontro SAW", TipoEvento.AO_VIVO, "Gestão financeira",
                Instant.parse("2026-08-01T19:00:00Z"), null, "https://meet.google.com/x", vagas);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    @Test
    void listarSoTrazEventosProgramadosOuAoVivo() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.buscarPorStatusIn(List.of(StatusEvento.PROGRAMADO, StatusEvento.AO_VIVO), null))
                .thenReturn(List.of(evento(UUID.randomUUID(), 50)));
        when(inscricaoEventoRepository.findByMentoradoId(mentorado.getId())).thenReturn(List.of());

        List<EventoMentoradoResponse> resultado = service().listar(usuarioId, null, null);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void listarFiltraPorTemaCaseInsensitive() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Evento comTema = evento(UUID.randomUUID(), 50);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.buscarPorStatusIn(List.of(StatusEvento.PROGRAMADO, StatusEvento.AO_VIVO), null))
                .thenReturn(List.of(comTema));
        when(inscricaoEventoRepository.findByMentoradoId(mentorado.getId())).thenReturn(List.of());

        assertThat(service().listar(usuarioId, null, "GESTÃO")).hasSize(1);
        assertThat(service().listar(usuarioId, null, "marketing")).isEmpty();
    }

    @Test
    void listarMarcaInscritoParaEventoComInscricaoAtiva() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Evento evento = evento(UUID.randomUUID(), 50);
        InscricaoEvento inscricao = new InscricaoEvento(mentorado, evento);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.buscarPorStatusIn(List.of(StatusEvento.PROGRAMADO, StatusEvento.AO_VIVO), null))
                .thenReturn(List.of(evento));
        when(inscricaoEventoRepository.findByMentoradoId(mentorado.getId())).thenReturn(List.of(inscricao));

        assertThat(service().listar(usuarioId, null, null).get(0).inscrito()).isTrue();
    }

    @Test
    void listarNaoMarcaInscritoParaInscricaoCancelada() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Evento evento = evento(UUID.randomUUID(), 50);
        InscricaoEvento inscricao = new InscricaoEvento(mentorado, evento);
        inscricao.cancelar();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.buscarPorStatusIn(List.of(StatusEvento.PROGRAMADO, StatusEvento.AO_VIVO), null))
                .thenReturn(List.of(evento));
        when(inscricaoEventoRepository.findByMentoradoId(mentorado.getId())).thenReturn(List.of(inscricao));

        assertThat(service().listar(usuarioId, null, null).get(0).inscrito()).isFalse();
    }

    @Test
    void inscreverOcupaVagaECriaInscricao() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Evento evento = evento(eventoId, 50);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscrever(usuarioId, eventoId);

        assertThat(resposta.inscrito()).isTrue();
        assertThat(evento.getVagasOcupadas()).isEqualTo(1);
    }

    @Test
    void inscreverJaInscritoEIdempotenteNaoOcupaSegundaVaga() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Evento evento = evento(eventoId, 50);
        InscricaoEvento jaInscrita = new InscricaoEvento(mentorado, evento);
        evento.ocuparVaga();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.of(jaInscrita));

        service().inscrever(usuarioId, eventoId);

        assertThat(evento.getVagasOcupadas()).isEqualTo(1); // não subiu pra 2
    }

    @Test
    void inscreverAposCancelarReativaAMesmaInscricao() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Evento evento = evento(eventoId, 50);
        InscricaoEvento cancelada = new InscricaoEvento(mentorado, evento);
        cancelada.cancelar();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.of(cancelada));
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().inscrever(usuarioId, eventoId);

        assertThat(cancelada.getStatus()).isEqualTo(StatusInscricao.INSCRITA);
    }

    @Test
    void inscreverEmEventoRealizadoLancaErro() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Evento evento = evento(eventoId, 50);
        evento.iniciar();
        evento.finalizar();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado(UUID.randomUUID())));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service().inscrever(usuarioId, eventoId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void inscreverSemVagaLancaErro() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Evento evento = evento(eventoId, 1);
        evento.ocuparVaga();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado(UUID.randomUUID())));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().inscrever(usuarioId, eventoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem vagas");
    }

    @Test
    void inscreverEmEventoComVagasNullNuncaBloqueiaPorCapacidade() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Evento evento = evento(eventoId, null);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado(UUID.randomUUID())));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(any(), any())).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscrever(usuarioId, eventoId);

        assertThat(resposta.vagasDisponiveis()).isNull();
    }

    @Test
    void cancelarLiberaVagaEMarcaCancelada() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        Evento evento = evento(eventoId, 50);
        InscricaoEvento inscricao = new InscricaoEvento(mentorado, evento);
        evento.ocuparVaga();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.of(inscricao));
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().cancelar(usuarioId, eventoId);

        assertThat(inscricao.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(evento.getVagasOcupadas()).isEqualTo(0);
    }

    @Test
    void cancelarSemInscricaoAtivaLanca404() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado(UUID.randomUUID())));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().cancelar(usuarioId, eventoId))
                .isInstanceOf(NoSuchElementException.class);
    }
}
