package com.techdoc.translator.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
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
        return chat(systemPrompt, userPrompt, 0);
    }

    /**
     * @param maxTokens 최대 출력 토큰 수. 0 이하면 제한하지 않는다.
     *                  제한에 걸려 답이 잘리면 LlmTruncatedException을 던진다
     *                  (끝나지 않는 출력을 읽기 시간 제한까지 기다리지 않기 위함).
     */
    public String chat(String systemPrompt, String userPrompt, int maxTokens) {
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.2);
        if (maxTokens > 0) {
            options.put("num_predict", maxTokens);
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "options", options,
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
            if (maxTokens > 0 && response != null && "length".equals(response.get("done_reason"))) {
                throw new LlmTruncatedException("LLM 출력이 최대 길이(" + maxTokens + " 토큰)에 걸려 잘렸습니다.");
            }
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
