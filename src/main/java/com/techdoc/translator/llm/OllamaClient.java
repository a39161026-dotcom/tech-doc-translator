package com.techdoc.translator.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Ollama /api/chat 호출 담당. 번역기와 요약기가 함께 쓴다.
 * 실패(연결 불가, 시간 초과, 빈 응답)는 모두 LlmException 으로 던진다.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama")
public class OllamaClient {

    private final RestClient restClient;
    private final String model;

    public OllamaClient(@Value("${llm.base-url}") String baseUrl,
                        @Value("${llm.model}") String model,
                        @Value("${llm.timeout-seconds:300}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.model = model;
    }

    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "options", Map.of("temperature", 0.2),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            Object message = response == null ? null : response.get("message");
            Object content = message instanceof Map<?, ?> m ? m.get("content") : null;
            if (content == null || content.toString().isBlank()) {
                throw new LlmException("LLM 응답이 비어 있습니다.");
            }
            return content.toString().trim();
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("LLM 호출 실패: " + e.getMessage(), e);
        }
    }
}
