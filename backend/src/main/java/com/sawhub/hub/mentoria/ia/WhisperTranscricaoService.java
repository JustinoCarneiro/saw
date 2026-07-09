package com.sawhub.hub.mentoria.ia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Implementação real do transcritor (Whisper API, decisão do ROADMAP.md M06). Gated por
 * config: sem {@code OPENAI_API_KEY} configurada, falha rápido e claro em vez de tentar uma
 * chamada HTTP fadada a dar 401 — quem chama (AtaProcessamentoService) já sabe tratar essa
 * exceção marcando a ata como FALHA. */
@Service
public class WhisperTranscricaoService implements TranscricaoService {

    private static final String ENDPOINT = "https://api.openai.com/v1/audio/transcriptions";

    private final String apiKey;
    private final RestClient restClient;

    public WhisperTranscricaoService(@Value("${sawhub.ia.openai-api-key:}") String apiKey, RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        // Achado (alto) da revisão de segurança do M06: sem timeout, uma Whisper API lenta/instável
        // trava a thread do ataProcessamentoExecutor (pool de só 2-4 threads) indefinidamente — a
        // ata fica presa em PROCESSANDO pra sempre (iniciarProcessamento()/publicar() bloqueiam
        // nesse estado). Read timeout generoso (áudio + transcrição podem levar minutos), connect
        // timeout curto (falha rápido se a API estiver fora do ar).
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofMinutes(3).toMillis());
        // Builder autoconfigurado do Spring Boot (não RestClient.create() direto) — traz o
        // ObjectMapper já configurado com FAIL_ON_UNKNOWN_PROPERTIES=false, sem isso qualquer
        // campo novo na resposta da OpenAI quebraria a desserialização.
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @Override
    public String transcrever(Path arquivoAudio) {
        if (apiKey.isBlank()) {
            throw new IaIndisponivelException("OPENAI_API_KEY não configurada — transcrição indisponível.");
        }
        exigirArquivoExistente(arquivoAudio);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(arquivoAudio));
        body.add("model", "whisper-1");

        try {
            WhisperResponse resposta = restClient.post()
                    .uri(ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(WhisperResponse.class);
            if (resposta == null || resposta.text() == null) {
                throw new IaIndisponivelException("Resposta vazia da Whisper API.");
            }
            return resposta.text();
        } catch (IaIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new IaIndisponivelException("Falha ao transcrever áudio via Whisper API.", e);
        }
    }

    /** Só pra validar que o arquivo existe antes de gastar uma chamada de API com ele. */
    private static void exigirArquivoExistente(Path arquivo) {
        if (!Files.exists(arquivo)) {
            throw new UncheckedIOException(new IOException("Arquivo de áudio não encontrado: " + arquivo));
        }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record WhisperResponse(String text) {
    }
}
