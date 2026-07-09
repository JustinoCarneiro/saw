package com.sawhub.hub.mentoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** Achado (médio) da revisão de segurança do M06: upload de áudio sem allow-list de
 * extensão/content-type permitia salvar em disco (e reencaminhar pra Whisper API) qualquer
 * binário com extensão escolhida pelo próprio cliente. */
class AudioStorageServiceTest {

    @TempDir
    Path tempDir;

    private AudioStorageService service() {
        return new AudioStorageService(tempDir.toString());
    }

    @Test
    void salvaArquivoDeAudioValido() throws IOException {
        var arquivo = new MockMultipartFile("arquivo", "gravacao.mp3", "audio/mpeg", "conteudo-fake".getBytes());

        String url = service().salvar(UUID.randomUUID(), arquivo);

        assertThat(url).endsWith(".mp3");
        assertThat(java.nio.file.Files.exists(tempDir.resolve(url))).isTrue();
    }

    @Test
    void rejeitaContentTypeQueNaoEDeAudio() {
        var arquivo = new MockMultipartFile("arquivo", "gravacao.mp3", "text/plain", "conteudo".getBytes());

        assertThatThrownBy(() -> service().salvar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("áudio");
    }

    @Test
    void rejeitaExtensaoForaDaAllowList() {
        var arquivo = new MockMultipartFile("arquivo", "script.php", "audio/mpeg", "conteudo".getBytes());

        assertThatThrownBy(() -> service().salvar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Extensão");
    }

    @Test
    void rejeitaArquivoSemContentType() {
        var arquivo = new MockMultipartFile("arquivo", "gravacao.mp3", null, "conteudo".getBytes());

        assertThatThrownBy(() -> service().salvar(UUID.randomUUID(), arquivo))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
