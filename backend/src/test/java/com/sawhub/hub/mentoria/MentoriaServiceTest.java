package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sawhub.hub.atividade.AtividadeLogService;
import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.ConteudoRepository;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentoria.dto.CriarMentoriaRequest;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import com.sawhub.hub.team.ColaboradorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    @Mock
    private ConteudoRepository conteudoRepository;
    @Mock
    private AtividadeLogService atividadeLogService;
    @Mock
    private PresencaMentoriaRepository presencaMentoriaRepository;

    private MentoriaService service() {
        return new MentoriaService(mentoriaRepository, colaboradorRepository, mentoradoRepository, conteudoRepository,
                atividadeLogService, presencaMentoriaRepository);
    }

    private static Conteudo conteudo(UUID id) {
        Conteudo c = new Conteudo("Ficha técnica", TipoConteudo.PLANILHA, "https://cdn.sawhub.com.br/x");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static Colaborador mentor(UUID id) {
        Colaborador c = new Colaborador(null, "Lucas", Area.GESTAO_PERFORMANCE);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static Mentorado mentorado(UUID id, String nome) {
        Mentorado m = new Mentorado(null, nome, null, BigDecimal.ZERO, 0, 0);
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
        when(mentoriaRepository.buscarPorStatus(null)).thenReturn(List.of(dentro, fora));

        List<Mentoria> resultado = service().listar(StatusMentoria.AGENDADA, null, null,
                Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-31T23:59:59Z"));

        assertThat(resultado).containsExactly(dentro);
    }

    // M28 (change request, 21/07/2026) — "reorganizar lista de mentorias": tipo filtra em memória
    // igual ao de/ate (mesmo motivo: dataset pequeno, sem valor em complicar a query SQL).
    @Test
    void listarComTipoFiltraEmMemoria() {
        Colaborador mentor = mentor(UUID.randomUUID());
        Mentoria grupo = new Mentoria(TipoMentoria.GRUPO, mentor,
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria"), mentorado(UUID.randomUUID(), "Ana")),
                Instant.parse("2026-07-15T12:00:00Z"), 60, null, null);
        Mentoria individual = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, java.util.Set.of(mentorado(UUID.randomUUID(), "João")),
                Instant.parse("2026-07-16T12:00:00Z"), 60, null, null);
        when(mentoriaRepository.buscarPorStatus(null)).thenReturn(List.of(grupo, individual));

        List<Mentoria> resultado = service().listar(null, TipoMentoria.GRUPO, null, null, null);

        assertThat(resultado).containsExactly(grupo);
    }

    // M28 — "aba própria do mentorado": mentoradoId != null troca a base da consulta pra
    // buscarPorMentorado (MEMBER OF), não buscarPorStatus.
    @Test
    void listarComMentoradoIdUsaBuscarPorMentorado() {
        UUID mentoradoId = UUID.randomUUID();
        Mentorado mentorado = mentorado(mentoradoId, "Maria");
        Colaborador mentor = mentor(UUID.randomUUID());
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, java.util.Set.of(mentorado),
                Instant.parse("2026-07-15T12:00:00Z"), 60, null, null);
        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of(mentoria));

        List<Mentoria> resultado = service().listar(null, null, mentoradoId, null, null);

        assertThat(resultado).containsExactly(mentoria);
        verify(mentoriaRepository, never()).buscarPorStatus(any());
    }

    @Test
    void listarComMentoradoIdInexistenteLancaErro() {
        UUID mentoradoId = UUID.randomUUID();
        when(mentoradoRepository.findById(mentoradoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().listar(null, null, mentoradoId, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
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
        // Confirmar não é marco do feed de atividades — só cancelar/realizar são.
        verifyNoInteractions(atividadeLogService);
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
    void cancelarAPartirDeConfirmadaRegistraAtividade() {
        UUID id = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")), Instant.now(), 60, null, null);
        mentoria.confirmar();
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mentoria cancelada = service().avancarStatus(id, StatusMentoria.CANCELADA);

        assertThat(cancelada.getStatus()).isEqualTo(StatusMentoria.CANCELADA);
        verify(atividadeLogService).registrar("MENTORIA_CANCELADA", "Mentoria cancelada: Maria");
    }

    @Test
    void atualizarMateriaisSubstituiListaInteira() {
        UUID id = UUID.randomUUID();
        UUID conteudoId = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")), Instant.now(), 60, null, null);
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(conteudoRepository.findAllById(List.of(conteudoId))).thenReturn(List.of(conteudo(conteudoId)));
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mentoria atualizada = service().atualizarMateriais(id, List.of(conteudoId));

        assertThat(atualizada.getMateriaisRecomendados()).hasSize(1);
        assertThat(atualizada.getMateriaisRecomendados().iterator().next().getId()).isEqualTo(conteudoId);
    }

    @Test
    void atualizarMateriaisComConteudoInexistenteLancaErro() {
        UUID id = UUID.randomUUID();
        UUID conteudoId = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")), Instant.now(), 60, null, null);
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(conteudoRepository.findAllById(List.of(conteudoId))).thenReturn(List.of());

        assertThatThrownBy(() -> service().atualizarMateriais(id, List.of(conteudoId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não foram encontrados");
    }

    @Test
    void atualizarMateriaisComListaVaziaLimpaAssociacoes() {
        UUID id = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(mentorado(UUID.randomUUID(), "Maria")), Instant.now(), 60, null, null);
        mentoria.atualizarMateriaisRecomendados(Set.of(conteudo(UUID.randomUUID())));
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(conteudoRepository.findAllById(List.of())).thenReturn(List.of());
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Mentoria atualizada = service().atualizarMateriais(id, List.of());

        assertThat(atualizada.getMateriaisRecomendados()).isEmpty();
    }

    // E17/M27 — presença por mentorado em mentoria GRUPO (ver ROADMAP.md § "Blueprint (M27)").
    @Test
    void registrarPresencasEmMentoriaGrupoSalvaUmRegistroPorMentorado() {
        UUID id = UUID.randomUUID();
        Mentorado maria = mentorado(UUID.randomUUID(), "Maria");
        Mentorado joao = mentorado(UUID.randomUUID(), "João");
        Mentoria mentoria = new Mentoria(TipoMentoria.GRUPO, mentor(UUID.randomUUID()),
                java.util.Set.of(maria, joao), Instant.now(), 60, null, null);
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(presencaMentoriaRepository.findByMentoriaIdAndMentoradoId(any(), any())).thenReturn(Optional.empty());
        when(presencaMentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest(List.of(
                new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest.PresencaRequest(maria.getId(), true),
                new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest.PresencaRequest(joao.getId(), false)));
        service().registrarPresencas(id, request);

        org.mockito.Mockito.verify(presencaMentoriaRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void registrarPresencasEmMentoriaIndividualLancaErro() {
        UUID id = UUID.randomUUID();
        Mentorado maria = mentorado(UUID.randomUUID(), "Maria");
        Mentoria mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(UUID.randomUUID()),
                java.util.Set.of(maria), Instant.now(), 60, null, null);
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));

        var request = new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest(List.of(
                new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest.PresencaRequest(maria.getId(), true)));

        assertThatThrownBy(() -> service().registrarPresencas(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("grupo");
        verifyNoInteractions(presencaMentoriaRepository);
    }

    @Test
    void registrarPresencasComMentoradoQueNaoParticipaLancaErro() {
        UUID id = UUID.randomUUID();
        Mentorado maria = mentorado(UUID.randomUUID(), "Maria");
        UUID outroId = UUID.randomUUID();
        Mentoria mentoria = new Mentoria(TipoMentoria.GRUPO, mentor(UUID.randomUUID()),
                java.util.Set.of(maria), Instant.now(), 60, null, null);
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));

        var request = new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest(List.of(
                new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest.PresencaRequest(outroId, true)));

        assertThatThrownBy(() -> service().registrarPresencas(id, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não participa");
    }

    @Test
    void registrarPresencasComRegistroJaExistenteAtualizaEmVezDeDuplicar() {
        UUID id = UUID.randomUUID();
        Mentorado maria = mentorado(UUID.randomUUID(), "Maria");
        Mentoria mentoria = new Mentoria(TipoMentoria.GRUPO, mentor(UUID.randomUUID()),
                java.util.Set.of(maria), Instant.now(), 60, null, null);
        PresencaMentoria existente = new PresencaMentoria(mentoria, maria, false);
        when(mentoriaRepository.buscarPorIdComDetalhes(id)).thenReturn(Optional.of(mentoria));
        when(presencaMentoriaRepository.findByMentoriaIdAndMentoradoId(id, maria.getId())).thenReturn(Optional.of(existente));
        when(presencaMentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest(List.of(
                new com.sawhub.hub.mentoria.dto.RegistrarPresencasRequest.PresencaRequest(maria.getId(), true)));
        service().registrarPresencas(id, request);

        org.mockito.Mockito.verify(presencaMentoriaRepository, org.mockito.Mockito.times(1)).save(existente);
        assertThat(existente.isPresente()).isTrue();
    }
}
