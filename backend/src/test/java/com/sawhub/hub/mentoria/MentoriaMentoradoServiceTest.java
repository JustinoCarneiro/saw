package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sawhub.hub.conteudo.Conteudo;
import com.sawhub.hub.conteudo.TipoConteudo;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.MentoradoRepository;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.mentoria.dto.MentoriaMentoradoResponse;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** H5.1-H5.3 — RED primeiro: MentoriaMentoradoService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class MentoriaMentoradoServiceTest {

    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private AtaRepository ataRepository;
    @Mock
    private MentoradoRepository mentoradoRepository;

    private MentoriaMentoradoService service() {
        return new MentoriaMentoradoService(mentoriaRepository, ataRepository, mentoradoRepository);
    }

    private static Mentorado mentorado(UUID id, Plano plano) {
        Mentorado m = new Mentorado(null, "Maria", null, plano, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private static Colaborador mentor() {
        return new Colaborador(null, "Brayan Silva", Area.GESTAO_PERFORMANCE);
    }

    private static Mentoria mentoria(UUID id, StatusMentoria status, Set<Conteudo> materiais) {
        Mentoria m = new Mentoria(TipoMentoria.INDIVIDUAL, mentor(), Set.of(), Instant.parse("2026-07-02T14:00:00Z"),
                60, "https://meet.google.com/x", null);
        ReflectionTestUtils.setField(m, "id", id);
        if (status == StatusMentoria.CONFIRMADA || status == StatusMentoria.REALIZADA) {
            m.confirmar();
        }
        if (status == StatusMentoria.REALIZADA) {
            m.realizar();
        }
        if (status == StatusMentoria.CANCELADA) {
            m.cancelar();
        }
        if (materiais != null) {
            m.atualizarMateriaisRecomendados(materiais);
        }
        return m;
    }

    private static Conteudo conteudo(UUID id, Plano planoMinimo, boolean publicado) {
        Conteudo c = new Conteudo("Ficha técnica", TipoConteudo.PLANILHA, "https://cdn.sawhub.com.br/x", planoMinimo);
        ReflectionTestUtils.setField(c, "id", id);
        if (publicado) {
            c.publicar();
        }
        return c;
    }

    @Test
    void listarResolveMentoradoAPartirDoUsuarioAutenticado() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.PROFISSIONAL);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of());
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(), StatusAta.PUBLICADA)).thenReturn(List.of());

        assertThat(service().listar(usuarioId)).isEmpty();
    }

    @Test
    void ataRascunhoNuncaApareceMesmoParaMentoriaRealizada() {
        UUID usuarioId = UUID.randomUUID();
        UUID mentoriaId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.PROFISSIONAL);
        Mentoria m = mentoria(mentoriaId, StatusMentoria.REALIZADA, null);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of(m));
        // AtaRepository já filtra status=PUBLICADA na query — uma ata RASCUNHO nunca é devolvida
        // aqui, o que simula exatamente o comportamento real (ver AtaRepository.findByMentoriaIdInAndStatus).
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(mentoriaId), StatusAta.PUBLICADA)).thenReturn(List.of());

        List<MentoriaMentoradoResponse> resultado = service().listar(usuarioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).ata()).isNull();
    }

    @Test
    void ataPublicadaApareceParaMentoriaRealizada() {
        UUID usuarioId = UUID.randomUUID();
        UUID mentoriaId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.PROFISSIONAL);
        Mentoria m = mentoria(mentoriaId, StatusMentoria.REALIZADA, null);
        Ata ata = new Ata(m);
        ata.concluirProcessamento("transcrição", "Resumo publicado.");
        ata.publicar();
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of(m));
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(mentoriaId), StatusAta.PUBLICADA)).thenReturn(List.of(ata));

        List<MentoriaMentoradoResponse> resultado = service().listar(usuarioId);

        assertThat(resultado.get(0).ata().resumo()).isEqualTo("Resumo publicado.");
    }

    @Test
    void materialNaoPublicadoNuncaApareceMesmoAssociado() {
        UUID usuarioId = UUID.randomUUID();
        UUID mentoriaId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.PROFISSIONAL);
        Conteudo rascunho = conteudo(UUID.randomUUID(), Plano.GRATUITO, false);
        Mentoria m = mentoria(mentoriaId, StatusMentoria.AGENDADA, Set.of(rascunho));
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of(m));
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(mentoriaId), StatusAta.PUBLICADA)).thenReturn(List.of());

        List<MentoriaMentoradoResponse> resultado = service().listar(usuarioId);

        assertThat(resultado.get(0).materiaisRecomendados()).isEmpty();
    }

    @Test
    void materialAcimaDoPlanoDoMentoradoNuncaAparece() {
        UUID usuarioId = UUID.randomUUID();
        UUID mentoriaId = UUID.randomUUID();
        Mentorado mentoradoGratuito = mentorado(UUID.randomUUID(), Plano.GRATUITO);
        Conteudo materialProfissional = conteudo(UUID.randomUUID(), Plano.PROFISSIONAL, true);
        Mentoria m = mentoria(mentoriaId, StatusMentoria.AGENDADA, Set.of(materialProfissional));
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentoradoGratuito));
        when(mentoriaRepository.buscarPorMentorado(mentoradoGratuito)).thenReturn(List.of(m));
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(mentoriaId), StatusAta.PUBLICADA)).thenReturn(List.of());

        List<MentoriaMentoradoResponse> resultado = service().listar(usuarioId);

        assertThat(resultado.get(0).materiaisRecomendados()).isEmpty();
    }

    @Test
    void materialPublicadoDentroDoPlanoAparece() {
        UUID usuarioId = UUID.randomUUID();
        UUID mentoriaId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.BASICO);
        Conteudo material = conteudo(UUID.randomUUID(), Plano.GRATUITO, true);
        Mentoria m = mentoria(mentoriaId, StatusMentoria.AGENDADA, Set.of(material));
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of(m));
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(mentoriaId), StatusAta.PUBLICADA)).thenReturn(List.of());

        List<MentoriaMentoradoResponse> resultado = service().listar(usuarioId);

        assertThat(resultado.get(0).materiaisRecomendados()).hasSize(1);
    }

    @Test
    void gerarIcsParaMentoriaQueNaoEDoMentoradoLanca404() {
        UUID usuarioId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.PROFISSIONAL);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of());
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(), StatusAta.PUBLICADA)).thenReturn(List.of());

        assertThatThrownBy(() -> service().gerarIcs(usuarioId, UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void gerarIcsParaMentoriaPropriaDevolveBytesComSummary() {
        UUID usuarioId = UUID.randomUUID();
        UUID mentoriaId = UUID.randomUUID();
        Mentorado mentorado = mentorado(UUID.randomUUID(), Plano.PROFISSIONAL);
        Mentoria m = mentoria(mentoriaId, StatusMentoria.CONFIRMADA, null);
        when(mentoradoRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(mentorado));
        when(mentoriaRepository.buscarPorMentorado(mentorado)).thenReturn(List.of(m));
        when(ataRepository.findByMentoriaIdInAndStatus(List.of(mentoriaId), StatusAta.PUBLICADA)).thenReturn(List.of());

        byte[] ics = service().gerarIcs(usuarioId, mentoriaId);

        assertThat(new String(ics)).contains("SUMMARY:Mentoria SAW HUB — Brayan Silva");
    }
}
