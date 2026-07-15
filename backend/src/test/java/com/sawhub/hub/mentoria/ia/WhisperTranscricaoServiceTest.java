package com.sawhub.hub.mentoria.ia;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Só o comportamento de fail-fast sem credencial — o resto (chamada HTTP de verdade) depende
 * de OPENAI_API_KEY real, fora do alcance de um teste de unidade (ver ROADMAP.md M06: "sinalizar
 * o que precisa de API key de verdade" na verificação ao vivo). */
class WhisperTranscricaoServiceTest {

    @Test
    void semApiKeyLancaIaIndisponivelSemTentarChamadaHttp() {
        var service = new WhisperTranscricaoService("", "https://api.openai.com", RestClient.builder());

        assertThatThrownBy(() -> service.transcrever(Path.of("qualquer.mp3")))
                .isInstanceOf(IaIndisponivelException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }
}
