package com.techdoc.translator.llm;

import com.sun.net.httpserver.HttpServer;
import com.techdoc.translator.document.SummaryLength;
import com.techdoc.translator.summary.OllamaSummarizer;
import com.techdoc.translator.summary.SummaryException;
import com.techdoc.translator.translation.OllamaTranslator;
import com.techdoc.translator.translation.TranslationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 가짜 Ollama 서버(JDK HttpServer)로 통신 구조만 검증한다. 모델 품질은 시험하지 않는다. */
class OllamaIntegrationTest {

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"안녕하세요\"}}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private OllamaClient client() {
        return new OllamaClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-model", 5);
    }

    @Test
    void 번역_요청을_보내고_응답_본문을_돌려준다() {
        OllamaTranslator translator = new OllamaTranslator(client());

        String result = translator.translate("Hello", Map.of());

        assertEquals("안녕하세요", result);
        assertEquals(1, requests.size());
        assertTrue(requests.get(0).contains("\"model\":\"test-model\""));
        assertTrue(requests.get(0).contains("\"stream\":false"));
        assertTrue(requests.get(0).contains("Hello"));
    }

    @Test
    void 용어집이_프롬프트에_들어간다() {
        OllamaTranslator translator = new OllamaTranslator(client());

        translator.translate("Use the dependency injection.", Map.of("dependency injection", "의존성 주입"));

        assertTrue(requests.get(0).contains("dependency injection => 의존성 주입"));
    }

    @Test
    void 서버가_오류를_주면_TranslationException() {
        status = 500;
        OllamaTranslator translator = new OllamaTranslator(client());

        assertThrows(TranslationException.class, () -> translator.translate("Hello", Map.of()));
    }

    @Test
    void 응답이_비어_있으면_TranslationException() {
        responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"  \"}}";
        OllamaTranslator translator = new OllamaTranslator(client());

        assertThrows(TranslationException.class, () -> translator.translate("Hello", Map.of()));
    }

    @Test
    void 서버에_연결할_수_없으면_TranslationException() {
        OllamaClient unreachable = new OllamaClient("http://127.0.0.1:1", "m", 2);
        OllamaTranslator translator = new OllamaTranslator(unreachable);

        assertThrows(TranslationException.class, () -> translator.translate("Hello", Map.of()));
    }

    @Test
    void 짧은_문서는_한_번에_요약한다() {
        OllamaSummarizer summarizer = new OllamaSummarizer(client(), 4000);

        String result = summarizer.summarize(List.of("문단1", "문단2"), SummaryLength.SHORT);

        assertEquals("안녕하세요", result);
        assertEquals(1, requests.size());
    }

    @Test
    void 긴_문서는_조각별_요약_후_다시_요약한다() {
        OllamaSummarizer summarizer = new OllamaSummarizer(client(), 10);

        summarizer.summarize(List.of("가나다라마바사", "아자차카타파하", "하하하하하하하"), SummaryLength.LONG);

        // 조각 3개 요약 + 최종 요약 1회
        assertEquals(4, requests.size());
    }

    @Test
    void 요약_중_LLM_오류는_SummaryException() {
        status = 500;
        OllamaSummarizer summarizer = new OllamaSummarizer(client(), 4000);

        assertThrows(SummaryException.class, () -> summarizer.summarize(List.of("문단"), SummaryLength.SHORT));
    }
}
