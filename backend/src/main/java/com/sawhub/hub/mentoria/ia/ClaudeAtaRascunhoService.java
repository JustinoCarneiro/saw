package com.sawhub.hub.mentoria.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Implementação real do gerador de rascunho (Claude Sonnet 5, decisão do ROADMAP.md M06) —
 * usa tool use pra forçar saída estruturada (resumo + encaminhamentos), em vez de tentar parsear
 * texto livre. Mesmo raciocínio de fail-fast do {@link WhisperTranscricaoService}: sem
 * {@code ANTHROPIC_API_KEY}, nem tenta a chamada. */
@Service
public class ClaudeAtaRascunhoService implements AtaRascunhoService {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String MODELO = "claude-sonnet-5";
    private static final String NOME_FERRAMENTA = "registrar_rascunho_ata";

    private final String apiKey;
    private final RestClient restClient;

    public ClaudeAtaRascunhoService(@Value("${sawhub.ia.anthropic-api-key:}") String apiKey,
                                     RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        // Achado (alto) da revisão de segurança do M06 — mesmo raciocínio do WhisperTranscricaoService:
        // sem timeout, uma Claude API lenta trava a thread do ataProcessamentoExecutor indefinidamente.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(90).toMillis());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @Override
    public RascunhoAta gerarRascunho(String transcricao) {
        if (apiKey.isBlank()) {
            throw new IaIndisponivelException("ANTHROPIC_API_KEY não configurada — geração de rascunho indisponível.");
        }

        Map<String, Object> requestBody = Map.of(
                "model", MODELO,
                "max_tokens", 2048,
                "tools", List.of(ferramentaRegistrarRascunho()),
                "tool_choice", Map.of("type", "tool", "name", NOME_FERRAMENTA),
                "messages", List.of(Map.of("role", "user", "content", prompt(transcricao)))
        );

        try {
            ClaudeResponse resposta = restClient.post()
                    .uri(ENDPOINT)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ClaudeResponse.class);

            Map<String, Object> input = extrairInput(resposta);
            return converterParaRascunho(input);
        } catch (IaIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new IaIndisponivelException("Falha ao gerar rascunho da ata via Claude.", e);
        }
    }

    private Map<String, Object> extrairInput(ClaudeResponse resposta) {
        if (resposta == null || resposta.content() == null) {
            throw new IaIndisponivelException("Resposta vazia da API da Anthropic.");
        }
        return resposta.content().stream()
                .filter(bloco -> NOME_FERRAMENTA.equals(bloco.name()) && bloco.input() != null)
                .map(ContentBlock::input)
                .findFirst()
                .orElseThrow(() -> new IaIndisponivelException("Claude não retornou o rascunho estruturado esperado."));
    }

    @SuppressWarnings("unchecked")
    private RascunhoAta converterParaRascunho(Map<String, Object> input) {
        String resumo = (String) input.get("resumo");
        List<Map<String, Object>> encaminhamentosRaw = (List<Map<String, Object>>) input.getOrDefault("encaminhamentos", List.of());
        List<RascunhoAta.EncaminhamentoSugerido> encaminhamentos = encaminhamentosRaw.stream()
                .map(e -> new RascunhoAta.EncaminhamentoSugerido((String) e.get("titulo"), ((Number) e.get("peso")).intValue()))
                .toList();
        return new RascunhoAta(resumo, encaminhamentos);
    }

    private static String prompt(String transcricao) {
        return """
                Você é assistente de um mentor de restaurantes (SAW). Leia a transcrição de uma \
                sessão de mentoria e registre, usando a ferramenta disponível: um resumo objetivo \
                (3-6 frases, em português, cobrindo os principais pontos discutidos) e uma lista \
                de encaminhamentos sugeridos (ações concretas que o mentorado deveria fazer até a \
                próxima mentoria). Para cada encaminhamento, defina peso 2 se for prioritário/urgente \
                ou peso 1 caso contrário. Não invente informação que não esteja na transcrição.

                Transcrição:
                %s
                """.formatted(transcricao);
    }

    private static Map<String, Object> ferramentaRegistrarRascunho() {
        return Map.of(
                "name", NOME_FERRAMENTA,
                "description", "Registra o resumo da mentoria e os encaminhamentos sugeridos.",
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "resumo", Map.of("type", "string"),
                                "encaminhamentos", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "titulo", Map.of("type", "string"),
                                                        "peso", Map.of("type", "integer", "enum", List.of(1, 2))
                                                ),
                                                "required", List.of("titulo", "peso")
                                        )
                                )
                        ),
                        "required", List.of("resumo", "encaminhamentos")
                )
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClaudeResponse(List<ContentBlock> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String name, Map<String, Object> input) {
    }
}
