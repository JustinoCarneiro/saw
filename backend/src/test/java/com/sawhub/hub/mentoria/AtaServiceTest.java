package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentorado.Encaminhamento;
import com.sawhub.hub.mentorado.EncaminhamentoRepository;
import com.sawhub.hub.mentorado.Mentorado;
import com.sawhub.hub.mentorado.Plano;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/** H5.2 + diferencial de IA — RED primeiro: AtaService ainda não existe neste ponto do ciclo. */
@ExtendWith(MockitoExtension.class)
class AtaServiceTest {

    @Mock
    private AtaRepository ataRepository;
    @Mock
    private MentoriaRepository mentoriaRepository;
    @Mock
    private AtaEncaminhamentoSugeridoRepository sugeridoRepository;
    @Mock
    private EncaminhamentoRepository encaminhamentoRepository;
    @Mock
    private AudioStorageService audioStorageService;
    @Mock
    private AtaProcessamentoService ataProcessamentoService;

    private AtaService service() {
        return new AtaService(ataRepository, mentoriaRepository, sugeridoRepository, encaminhamentoRepository,
                audioStorageService, ataProcessamentoService);
    }

    private static Mentorado mentorado(String nome) {
        Mentorado m = new Mentorado(null, nome, null, Plano.ESSENCIAL, BigDecimal.ZERO, 0, 0);
        ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
        return m;
    }

    private static Mentoria mentoriaConfirmada(Set<Mentorado> mentorados) {
        Colaborador mentor = new Colaborador(null, "Lucas", Area.GESTAO_PERFORMANCE, 10, BigDecimal.TEN);
        Mentoria m = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, mentorados, Instant.now(), 60, null, null);
        m.confirmar();
        return m;
    }

    @Test
    void realizarMentoriaTransicionaECriaAtaVazia() {
        UUID mentoriaId = UUID.randomUUID();
        Mentoria mentoria = mentoriaConfirmada(Set.of(mentorado("Maria")));
        when(mentoriaRepository.buscarPorIdComDetalhes(mentoriaId)).thenReturn(Optional.of(mentoria));
        when(mentoriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Ata ata = service().realizarMentoria(mentoriaId);

        assertThat(mentoria.getStatus()).isEqualTo(StatusMentoria.REALIZADA);
        assertThat(ata.getStatusProcessamento()).isEqualTo(StatusProcessamentoAta.SEM_AUDIO);
        assertThat(ata.getStatus()).isEqualTo(StatusAta.RASCUNHO);
    }

    @Test
    void iniciarUploadSalvaArquivoEDisparaProcessamentoAssincrono() {
        UUID mentoriaId = UUID.randomUUID();
        Mentoria mentoria = mentoriaConfirmada(Set.of(mentorado("Maria")));
        mentoria.realizar();
        Ata ata = new Ata(mentoria);
        ReflectionTestUtils.setField(ata, "id", UUID.randomUUID());
        when(ataRepository.findByMentoriaId(mentoriaId)).thenReturn(Optional.of(ata));
        when(audioStorageService.salvar(any(), any())).thenReturn("audio.mp3");
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var arquivo = new MockMultipartFile("arquivo", "gravacao.mp3", "audio/mpeg", "conteudo".getBytes());
        Ata salva = service().iniciarUpload(mentoriaId, arquivo);

        assertThat(salva.getStatusProcessamento()).isEqualTo(StatusProcessamentoAta.PROCESSANDO);
        verify(ataProcessamentoService).processar(ata.getId(), "audio.mp3");
    }

    @Test
    void editarResumoAtualizaTexto() {
        UUID mentoriaId = UUID.randomUUID();
        Mentoria mentoria = mentoriaConfirmada(Set.of(mentorado("Maria")));
        mentoria.realizar();
        Ata ata = new Ata(mentoria);
        when(ataRepository.findByMentoriaId(mentoriaId)).thenReturn(Optional.of(ata));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Ata editada = service().editarResumo(mentoriaId, "Resumo escrito manualmente.");

        assertThat(editada.getResumo()).isEqualTo("Resumo escrito manualmente.");
    }

    @Test
    void editarResumoDeAtaJaPublicadaLancaErro() {
        UUID mentoriaId = UUID.randomUUID();
        Mentoria mentoria = mentoriaConfirmada(Set.of(mentorado("Maria")));
        mentoria.realizar();
        Ata ata = new Ata(mentoria);
        ata.publicar();
        when(ataRepository.findByMentoriaId(mentoriaId)).thenReturn(Optional.of(ata));

        assertThatThrownBy(() -> service().editarResumo(mentoriaId, "Novo resumo"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publicarMaterializaSugestoesAceitasEmEncaminhamentoPorMentorado() {
        UUID mentoriaId = UUID.randomUUID();
        Mentorado maria = mentorado("Maria");
        Mentorado joao = mentorado("João");
        Mentoria mentoria = mentoriaConfirmada(Set.of(maria, joao));
        mentoria.realizar();
        Ata ata = new Ata(mentoria);
        ReflectionTestUtils.setField(ata, "id", UUID.randomUUID());

        AtaEncaminhamentoSugerido aceita = new AtaEncaminhamentoSugerido(ata, "Atualizar ficha técnica", 2, true);
        AtaEncaminhamentoSugerido rejeitada = new AtaEncaminhamentoSugerido(ata, "Item descartado", 1, false);

        when(mentoriaRepository.buscarPorIdComDetalhes(mentoriaId)).thenReturn(Optional.of(mentoria));
        when(ataRepository.findByMentoriaId(mentoriaId)).thenReturn(Optional.of(ata));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sugeridoRepository.findByAtaIdOrderByTituloAsc(ata.getId())).thenReturn(List.of(aceita, rejeitada));

        Ata publicada = service().publicar(mentoriaId);

        assertThat(publicada.getStatus()).isEqualTo(StatusAta.PUBLICADA);
        // 1 sugestão aceita x 2 mentorados da mentoria = 2 encaminhamentos criados.
        verify(encaminhamentoRepository, times(2)).save(any(Encaminhamento.class));
    }

    @Test
    void publicarSemSugestoesAceitasNaoCriaEncaminhamento() {
        UUID mentoriaId = UUID.randomUUID();
        Mentoria mentoria = mentoriaConfirmada(Set.of(mentorado("Maria")));
        mentoria.realizar();
        Ata ata = new Ata(mentoria);
        ReflectionTestUtils.setField(ata, "id", UUID.randomUUID());

        when(mentoriaRepository.buscarPorIdComDetalhes(mentoriaId)).thenReturn(Optional.of(mentoria));
        when(ataRepository.findByMentoriaId(mentoriaId)).thenReturn(Optional.of(ata));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sugeridoRepository.findByAtaIdOrderByTituloAsc(ata.getId())).thenReturn(List.of());

        service().publicar(mentoriaId);

        verify(encaminhamentoRepository, never()).save(any());
    }

    @Test
    void editarSugestaoDeOutraAtaLancaErro() {
        UUID mentoriaId = UUID.randomUUID();
        Mentoria mentoria = mentoriaConfirmada(Set.of(mentorado("Maria")));
        mentoria.realizar();
        Ata ataDestino = new Ata(mentoria);
        ReflectionTestUtils.setField(ataDestino, "id", UUID.randomUUID());

        Ata outraAta = mock(Ata.class);
        when(outraAta.getId()).thenReturn(UUID.randomUUID());
        AtaEncaminhamentoSugerido sugestaoDeOutraAta = new AtaEncaminhamentoSugerido(outraAta, "X", 1, true);
        ReflectionTestUtils.setField(sugestaoDeOutraAta, "id", UUID.randomUUID());

        when(ataRepository.findByMentoriaId(mentoriaId)).thenReturn(Optional.of(ataDestino));
        when(sugeridoRepository.findById(sugestaoDeOutraAta.getId())).thenReturn(Optional.of(sugestaoDeOutraAta));

        assertThatThrownBy(() -> service().editarSugestao(mentoriaId, sugestaoDeOutraAta.getId(), "Y", 1, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");
    }

    @Test
    void editarSugestaoDeAtaJaPublicadaLancaErro() {
        // Achado (baixo) da revisão de segurança do M06: sem Ata.exigirRascunho() aqui, uma
        // sugestão continuava editável depois da publicação, divergindo do que já foi
        // materializado em Encaminhamento.
        UUID mentoriaId = UUID.randomUUID();
        Mentoria mentoria = mentoriaConfirmada(Set.of(mentorado("Maria")));
        mentoria.realizar();
        Ata ata = new Ata(mentoria);
        ata.publicar();
        when(ataRepository.findByMentoriaId(mentoriaId)).thenReturn(Optional.of(ata));

        assertThatThrownBy(() -> service().editarSugestao(mentoriaId, UUID.randomUUID(), "Y", 1, true))
                .isInstanceOf(IllegalStateException.class);
    }
}
