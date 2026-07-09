package com.sawhub.hub.mentoria.ia;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Só o comportamento de fail-fast sem credencial — ver nota equivalente em
 * WhisperTranscricaoServiceTest. */
class ClaudeAtaRascunhoServiceTest {

    @Test
    void semApiKeyLancaIaIndisponivelSemTentarChamadaHttp() {
        var service = new ClaudeAtaRascunhoService("", RestClient.builder());

        assertThatThrownBy(() -> service.gerarRascunho("transcrição qualquer"))
                .isInstanceOf(IaIndisponivelException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }
}
