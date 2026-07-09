package com.sawhub.hub.evento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sawhub.hub.evento.dto.CriarEventoRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** H11.4 — RED primeiro: EventoService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    private EventoService service() {
        return new EventoService(eventoRepository);
    }

    private static Evento evento(StatusEvento status) {
        Evento e = new Evento("Encontro SAW", TipoEvento.AO_VIVO, "Gestão financeira",
                Instant.parse("2026-08-01T19:00:00Z"), null, "https://meet.google.com/x", 50);
        if (status == StatusEvento.AO_VIVO) {
            e.iniciar();
        } else if (status == StatusEvento.REALIZADO) {
            e.iniciar();
            e.finalizar();
        } else if (status == StatusEvento.CANCELADO) {
            e.cancelar();
        }
        return e;
    }

    @Test
    void criarPersisteEventoProgramado() {
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarEventoRequest("Encontro SAW", TipoEvento.PRESENCIAL, "Marketing digital",
                Instant.parse("2026-09-01T19:00:00Z"), "Av. Paulista, 1000", null, 30);

        Evento criado = service().criar(request);

        assertThat(criado.getStatus()).isEqualTo(StatusEvento.PROGRAMADO);
        assertThat(criado.getTitulo()).isEqualTo("Encontro SAW");
    }

    @Test
    void avancarParaAoVivoAPartirDeProgramado() {
        UUID id = UUID.randomUUID();
        Evento e = evento(StatusEvento.PROGRAMADO);
        when(eventoRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evento atualizado = service().avancarStatus(id, StatusEvento.AO_VIVO);

        assertThat(atualizado.getStatus()).isEqualTo(StatusEvento.AO_VIVO);
    }

    @Test
    void avancarParaRealizadoPulandoAoVivoLancaErro() {
        UUID id = UUID.randomUUID();
        Evento e = evento(StatusEvento.PROGRAMADO);
        when(eventoRepository.findById(id)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service().avancarStatus(id, StatusEvento.REALIZADO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelarAPartirDeAoVivoFunciona() {
        UUID id = UUID.randomUUID();
        Evento e = evento(StatusEvento.AO_VIVO);
        when(eventoRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evento cancelado = service().avancarStatus(id, StatusEvento.CANCELADO);

        assertThat(cancelado.getStatus()).isEqualTo(StatusEvento.CANCELADO);
    }

    @Test
    void cancelarEventoJaRealizadoLancaErro() {
        UUID id = UUID.randomUUID();
        Evento e = evento(StatusEvento.REALIZADO);
        when(eventoRepository.findById(id)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service().avancarStatus(id, StatusEvento.CANCELADO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void voltarParaProgramadoLancaErro() {
        UUID id = UUID.randomUUID();
        Evento e = evento(StatusEvento.AO_VIVO);
        when(eventoRepository.findById(id)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service().avancarStatus(id, StatusEvento.PROGRAMADO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
