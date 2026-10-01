package com.safrapay.backend.chat;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente mínimo da API de mensagens da Anthropic (Claude), via HTTP direto
 * (RestClient do Spring) — sem SDK extra no pom.
 *
 * A chave vem da variável de ambiente ANTHROPIC_API_KEY (só no .env da EC2,
 * nunca no app nem no git). Sem chave, o assistente responde 503 e o resto do
 * backend funciona normalmente.
 */
@Component
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String URL = "https://api.anthropic.com/v1/messages";
    private static final String VERSAO_API = "2023-06-01";

    private final RestClient http;
    private final String apiKey;
    private final String modelo;
    private final int maxTokens;

    public AnthropicClient(RestClient.Builder builder,
                           @Value("${ia.anthropic.api-key:}") String apiKey,
                           @Value("${ia.modelo}") String modelo,
                           @Value("${ia.max-tokens:1024}") int maxTokens) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(10_000);
        fabrica.setReadTimeout(60_000);
        this.http = builder.requestFactory(fabrica).build();
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.maxTokens = maxTokens;
    }

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Chama o modelo obrigando o uso de uma ferramenta ("tool use") e devolve
     * o "input" que o modelo preencheu — é assim que se garante uma resposta
     * estruturada (JSON no formato do input_schema da ferramenta).
     *
     * @param sistema   prompt de sistema
     * @param mensagens lista de {"role": "user"|"assistant", "content": "..."}
     * @param ferramenta definição da ferramenta (name, description, input_schema)
     */
    public JsonNode chamarComFerramenta(String sistema, List<Map<String, Object>> mensagens,
                                       Map<String, Object> ferramenta) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", modelo);
        corpo.put("max_tokens", maxTokens);
        corpo.put("system", sistema);
        corpo.put("messages", mensagens);
        corpo.put("tools", List.of(ferramenta));
        corpo.put("tool_choice", Map.of("type", "tool", "name", ferramenta.get("name")));

        JsonNode resposta = http.post()
                .uri(URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", VERSAO_API)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);

        if (resposta == null) throw new IllegalStateException("Resposta vazia da API de IA");

        JsonNode uso = resposta.path("usage");
        log.info("IA chamada: modelo={} tokensEntrada={} tokensSaida={}",
                modelo, uso.path("input_tokens").asInt(), uso.path("output_tokens").asInt());

        for (JsonNode bloco : resposta.path("content")) {
            if ("tool_use".equals(bloco.path("type").asText())) return bloco.path("input");
        }
        throw new IllegalStateException("Resposta da IA sem tool_use (stop_reason="
                + resposta.path("stop_reason").asText() + ")");
    }
}
