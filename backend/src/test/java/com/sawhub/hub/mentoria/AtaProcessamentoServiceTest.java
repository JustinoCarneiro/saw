package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sawhub.hub.mentoria.ia.AtaRascunhoService;
import com.sawhub.hub.mentoria.ia.RascunhoAta;
import com.sawhub.hub.mentoria.ia.TranscricaoService;
import com.sawhub.hub.team.Area;
import com.sawhub.hub.team.Colaborador;
import java.nio.file.Path;
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

/** Chama {@code processar()} diretamente (sem `@Async` real) — o que importa aqui é a lógica de
 * orquestração (transcrever -> gerar rascunho -> persistir, ou capturar falha), não o
 * comportamento de thread pool do Spring (isso é infra, não testável de forma útil com Mockito). */
@ExtendWith(MockitoExtension.class)
class AtaProcessamentoServiceTest {

    @Mock
    private AtaRepository ataRepository;
    @Mock
    private AtaEncaminhamentoSugeridoRepository sugeridoRepository;
    @Mock
    private TranscricaoService transcricaoService;
    @Mock
    private AtaRascunhoService ataRascunhoService;
    @Mock
    private AudioStorageService audioStorageService;

    private AtaProcessamentoService service() {
        return new AtaProcessamentoService(ataRepository, sugeridoRepository, transcricaoService,
                ataRascunhoService, audioStorageService);
    }

    private static Ata ataProcessando() {
        Colaborador mentor = new Colaborador(null, "Lucas", Area.GESTAO_PERFORMANCE);
        var mentoria = new Mentoria(TipoMentoria.INDIVIDUAL, mentor, Set.of(), Instant.now(), 60, null, null);
        mentoria.confirmar();
        mentoria.realizar();
        Ata ata = new Ata(mentoria);
        ReflectionTestUtils.setField(ata, "id", UUID.randomUUID());
        ata.iniciarProcessamento("audio.mp3");
        return ata;
    }

    @Test
    void processarComSucessoConcluiEPersisteSugestoes() {
        Ata ata = ataProcessando();
        when(audioStorageService.resolver("audio.mp3")).thenReturn(Path.of("/tmp/audio.mp3"));
        when(transcricaoService.transcrever(any())).thenReturn("transcrição da mentoria");
        when(ataRascunhoService.gerarRascunho("transcrição da mentoria")).thenReturn(
                new RascunhoAta("Resumo gerado pela IA", "Decisões geradas pela IA", List.of(
                        new RascunhoAta.EncaminhamentoSugerido("Atualizar ficha técnica", 2))));
        when(ataRepository.findById(ata.getId())).thenReturn(Optional.of(ata));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().processar(ata.getId(), "audio.mp3");

        assertThat(ata.getStatusProcessamento()).isEqualTo(StatusProcessamentoAta.CONCLUIDO);
        assertThat(ata.getResumo()).isEqualTo("Resumo gerado pela IA");
        assertThat(ata.getDecisoes()).isEqualTo("Decisões geradas pela IA");
    }

    @Test
    void processarComFalhaDaTranscricaoMarcaFalha() {
        Ata ata = ataProcessando();
        when(audioStorageService.resolver("audio.mp3")).thenReturn(Path.of("/tmp/audio.mp3"));
        when(transcricaoService.transcrever(any())).thenThrow(new RuntimeException("Whisper indisponível"));
        when(ataRepository.findById(ata.getId())).thenReturn(Optional.of(ata));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().processar(ata.getId(), "audio.mp3");

        assertThat(ata.getStatusProcessamento()).isEqualTo(StatusProcessamentoAta.FALHA);
        assertThat(ata.getErroProcessamento()).isEqualTo("Whisper indisponível");
    }

    // M28 (change request, 21/07/2026) — "colar transcrição do Google Meet": mesma orquestração
    // de processar(), mas pula transcricaoService/audioStorageService por completo.
    @Test
    void processarTranscricaoColadaComSucessoConcluiEPersisteSugestoesSemChamarTranscricaoService() {
        Ata ata = ataProcessando();
        when(ataRascunhoService.gerarRascunho("transcrição colada do Meet")).thenReturn(
                new RascunhoAta("Resumo gerado pela IA", "Decisões geradas pela IA", List.of(
                        new RascunhoAta.EncaminhamentoSugerido("Atualizar ficha técnica", 2))));
        when(ataRepository.findById(ata.getId())).thenReturn(Optional.of(ata));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().processarTranscricaoColada(ata.getId(), "transcrição colada do Meet");

        assertThat(ata.getStatusProcessamento()).isEqualTo(StatusProcessamentoAta.CONCLUIDO);
        assertThat(ata.getTranscricao()).isEqualTo("transcrição colada do Meet");
        assertThat(ata.getResumo()).isEqualTo("Resumo gerado pela IA");
        verifyNoInteractions(transcricaoService, audioStorageService);
    }

    @Test
    void processarTranscricaoColadaComFalhaDoRascunhoMarcaFalha() {
        Ata ata = ataProcessando();
        when(ataRascunhoService.gerarRascunho(any())).thenThrow(new RuntimeException("Claude indisponível"));
        when(ataRepository.findById(ata.getId())).thenReturn(Optional.of(ata));
        when(ataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().processarTranscricaoColada(ata.getId(), "transcrição colada do Meet");

        assertThat(ata.getStatusProcessamento()).isEqualTo(StatusProcessamentoAta.FALHA);
        assertThat(ata.getErroProcessamento()).isEqualTo("Claude indisponível");
    }
}
