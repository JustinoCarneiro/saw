package com.sawhub.hub.evento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.evento.dto.EventoMentoradoResponse;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentorado.TipoContrato;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // --- M28: cota de 3 eventos grátis/ano de contrato (Mentoria Contínua) ---

    private static Mentorado mentoradoContinua(UUID id, LocalDate dataFechamentoContrato) {
        Mentorado m = mentorado(id);
        m.atualizarDadosContrato(null, null, null, TipoContrato.MENTORIA_CONTINUA, null, dataFechamentoContrato);
        return m;
    }

    private static InscricaoEvento inscricaoEmData(Mentorado mentorado, LocalDate dataEvento) {
        Evento evento = new Evento("Evento do ciclo", TipoEvento.AO_VIVO, null,
                dataEvento.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), null, null, null);
        ReflectionTestUtils.setField(evento, "id", UUID.randomUUID());
        return new InscricaoEvento(mentorado, evento);
    }

    @Test
    void inscreverContinuaComTresEventosNoCicloAtualBloqueiaOQuarto() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        LocalDate hoje = LocalDate.now();
        Mentorado mentorado = mentoradoContinua(UUID.randomUUID(), hoje.minusMonths(6));
        Evento novoEvento = evento(eventoId, 50);
        List<InscricaoEvento> jaUsadas = List.of(
                inscricaoEmData(mentorado, hoje.minusMonths(1)),
                inscricaoEmData(mentorado, hoje.minusMonths(2)),
                inscricaoEmData(mentorado, hoje.minusMonths(3)));

        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(novoEvento));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.buscarPorMentoradoComEvento(mentorado.getId())).thenReturn(jaUsadas);

        assertThatThrownBy(() -> service().inscrever(usuarioId, eventoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cota de 3");
    }

    @Test
    void inscreverContinuaComMenosDeTresNoCicloAtualPermite() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        LocalDate hoje = LocalDate.now();
        Mentorado mentorado = mentoradoContinua(UUID.randomUUID(), hoje.minusMonths(6));
        Evento novoEvento = evento(eventoId, 50);
        List<InscricaoEvento> jaUsadas = List.of(inscricaoEmData(mentorado, hoje.minusMonths(1)));

        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(novoEvento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.buscarPorMentoradoComEvento(mentorado.getId())).thenReturn(jaUsadas);
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscrever(usuarioId, eventoId);

        assertThat(resposta.inscrito()).isTrue();
    }

    // Janela rolante de CONTRATO, não ano civil: 3 eventos usados no ciclo ANTERIOR (antes do
    // último aniversário do contrato) não contam pro ciclo atual — a cota renova a cada 12 meses.
    @Test
    void inscreverContinuaIgnoraEventosDeCicloDeContratoAnterior() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        LocalDate hoje = LocalDate.now();
        LocalDate dataFechamento = hoje.minusMonths(6); // ciclo atual começou há 6 meses
        Mentorado mentorado = mentoradoContinua(UUID.randomUUID(), dataFechamento);
        Evento novoEvento = evento(eventoId, 50);
        // 3 eventos ANTES do início do ciclo atual (ciclo de contrato anterior).
        List<InscricaoEvento> doCicloAnterior = List.of(
                inscricaoEmData(mentorado, dataFechamento.minusMonths(1)),
                inscricaoEmData(mentorado, dataFechamento.minusMonths(3)),
                inscricaoEmData(mentorado, dataFechamento.minusMonths(5)));

        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(novoEvento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.buscarPorMentoradoComEvento(mentorado.getId())).thenReturn(doCicloAnterior);
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscrever(usuarioId, eventoId);

        assertThat(resposta.inscrito()).isTrue();
    }

    @Test
    void inscreverContinuaIgnoraInscricoesCanceladasNaContagemDaCota() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        LocalDate hoje = LocalDate.now();
        Mentorado mentorado = mentoradoContinua(UUID.randomUUID(), hoje.minusMonths(6));
        Evento novoEvento = evento(eventoId, 50);
        InscricaoEvento canceladas1 = inscricaoEmData(mentorado, hoje.minusMonths(1));
        canceladas1.cancelar();
        InscricaoEvento canceladas2 = inscricaoEmData(mentorado, hoje.minusMonths(2));
        canceladas2.cancelar();
        InscricaoEvento canceladas3 = inscricaoEmData(mentorado, hoje.minusMonths(3));
        canceladas3.cancelar();

        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(novoEvento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.buscarPorMentoradoComEvento(mentorado.getId()))
                .thenReturn(List.of(canceladas1, canceladas2, canceladas3));
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscrever(usuarioId, eventoId);

        assertThat(resposta.inscrito()).isTrue();
    }

    @Test
    void inscreverContinuaSemDataFechamentoContratoNaoBloqueiaPorFaltaDeDado() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentoradoContinua(UUID.randomUUID(), null);
        Evento novoEvento = evento(eventoId, 50);

        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(novoEvento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscrever(usuarioId, eventoId);

        assertThat(resposta.inscrito()).isTrue();
        verify(inscricaoEventoRepository, never()).buscarPorMentoradoComEvento(any());
    }

    @Test
    void inscreverTipoContratoDiferenteDeContinuaNuncaEBloqueadoPelaCota() {
        UUID usuarioId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID());
        mentorado.atualizarDadosContrato(null, null, null, TipoContrato.MENTORIA_INDIVIDUAL, null, LocalDate.now().minusMonths(6));
        Evento novoEvento = evento(eventoId, 50);

        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(novoEvento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentorado.getId(), eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscrever(usuarioId, eventoId);

        assertThat(resposta.inscrito()).isTrue();
        verify(inscricaoEventoRepository, never()).buscarPorMentoradoComEvento(any());
    }

    // --- M28: caminho admin (mentoradoId por parâmetro, não usuário autenticado) ---

    @Test
    void inscreverAdminResolveMentoradoPorIdNaoPorUsuarioAutenticado() {
        UUID mentoradoId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(mentoradoId);
        Evento evento = evento(eventoId, 50);

        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.of(mentorado));
        when(eventoRepository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentoradoId, eventoId)).thenReturn(Optional.empty());
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoMentoradoResponse resposta = service().inscreverAdmin(mentoradoId, eventoId);

        assertThat(resposta.inscrito()).isTrue();
        verify(mentoradoRepository, never()).findByUsuarioId(any());
    }

    @Test
    void inscreverAdminComMentoradoInexistenteLancaErro() {
        UUID mentoradoId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().inscreverAdmin(mentoradoId, eventoId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void cancelarAdminLiberaVagaEMarcaCancelada() {
        UUID mentoradoId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(mentoradoId);
        Evento evento = evento(eventoId, 50);
        InscricaoEvento inscricao = new InscricaoEvento(mentorado, evento);
        evento.ocuparVaga();

        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.of(mentorado));
        when(inscricaoEventoRepository.findByMentoradoIdAndEventoId(mentoradoId, eventoId)).thenReturn(Optional.of(inscricao));
        when(inscricaoEventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().cancelarAdmin(mentoradoId, eventoId);

        assertThat(inscricao.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(evento.getVagasOcupadas()).isEqualTo(0);
    }

    @Test
    void listarInscricoesAdminDevolveOHistoricoDoMentorado() {
        UUID mentoradoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(mentoradoId);
        InscricaoEvento inscricao = new InscricaoEvento(mentorado, evento(UUID.randomUUID(), 50));

        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.of(mentorado));
        when(inscricaoEventoRepository.buscarPorMentoradoComEvento(mentoradoId)).thenReturn(new ArrayList<>(List.of(inscricao)));

        assertThat(service().listarInscricoesAdmin(mentoradoId)).containsExactly(inscricao);
    }

    @Test
    void listarInscricoesAdminComMentoradoInexistenteLancaErro() {
        UUID mentoradoId = UUID.randomUUID();
        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().listarInscricoesAdmin(mentoradoId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- M28: consultarCotaAdmin (resumo pro card "X/3 usados") ---

    @Test
    void consultarCotaAdminParaContinuaContaUsadasNoCicloAtual() {
        UUID mentoradoId = UUID.randomUUID();
        LocalDate hoje = LocalDate.now();
        LocalDate dataFechamento = hoje.minusMonths(6);
        Mentorado mentorado = mentoradoContinua(mentoradoId, dataFechamento);
        List<InscricaoEvento> usadas = List.of(
                inscricaoEmData(mentorado, hoje.minusMonths(1)),
                inscricaoEmData(mentorado, hoje.minusMonths(2)));

        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.of(mentorado));
        when(inscricaoEventoRepository.buscarPorMentoradoComEvento(mentoradoId)).thenReturn(usadas);

        var cota = service().consultarCotaAdmin(mentoradoId);

        assertThat(cota.aplicavel()).isTrue();
        assertThat(cota.usadas()).isEqualTo(2);
        assertThat(cota.limite()).isEqualTo(3);
        assertThat(cota.inicioCiclo()).isEqualTo(dataFechamento);
        assertThat(cota.fimCiclo()).isEqualTo(dataFechamento.plusYears(1));
    }

    @Test
    void consultarCotaAdminParaTipoDeContratoDiferenteNaoEAplicavel() {
        UUID mentoradoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(mentoradoId);
        mentorado.atualizarDadosContrato(null, null, null, TipoContrato.CONSULTORIA, null, LocalDate.now().minusMonths(6));
        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.of(mentorado));

        var cota = service().consultarCotaAdmin(mentoradoId);

        assertThat(cota.aplicavel()).isFalse();
        verify(inscricaoEventoRepository, never()).buscarPorMentoradoComEvento(any());
    }

    @Test
    void consultarCotaAdminSemDataFechamentoContratoNaoEAplicavel() {
        UUID mentoradoId = UUID.randomUUID();
        Mentorado mentorado = mentoradoContinua(mentoradoId, null);
        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.of(mentorado));

        var cota = service().consultarCotaAdmin(mentoradoId);

        assertThat(cota.aplicavel()).isFalse();
    }
}
