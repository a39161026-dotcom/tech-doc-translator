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
    private volatile String firstResponseBody = null;
    private volatile String responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"안녕하세요\"}}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = (requests.size() == 1 && firstResponseBody != null) ? firstResponseBody : responseBody;
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
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

    @Test
    void 코드_블록은_LLM_호출_없이_그대로_돌려준다() {
        OllamaTranslator translator = new OllamaTranslator(client());
        String code = "```java\npublic interface MemberRepository {}\n```";

        String result = translator.translate(code, Map.of());

        assertEquals(code, result);
        assertEquals(0, requests.size());
    }

    @Test
    void 제목은_샵_표시를_유지하고_글자만_번역한다() {
        OllamaTranslator translator = new OllamaTranslator(client());

        String result = translator.translate("## Pagination", Map.of());

        assertEquals("## 안녕하세요", result);
        assertEquals(1, requests.size());
        assertTrue(requests.get(0).contains("Pagination"));
        assertTrue(!requests.get(0).contains("## Pagination"));
    }

    @Test
    void 모델이_제목_번역문에_샵을_또_붙여도_하나만_남긴다() {
        responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"## 애플리케이션 배포하기\"}}";
        OllamaTranslator translator = new OllamaTranslator(client());

        String result = translator.translate("# Deploying the Application", Map.of());

        assertEquals("# 애플리케이션 배포하기", result);
    }

    @Test
    void 원문에_없는_용어는_프롬프트에_넣지_않는다() {
        OllamaTranslator translator = new OllamaTranslator(client());

        translator.translate("Hello world.", Map.of("dirty checking", "더티 체킹"));

        assertTrue(!requests.get(0).contains("더티 체킹"));
    }

    @Test
    void 결과가_비정상적으로_길면_한_번_다시_시도하고_그래도_길면_실패한다() {
        responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"" + "가".repeat(300) + "\"}}";
        OllamaTranslator translator = new OllamaTranslator(client());

        assertThrows(TranslationException.class, () -> translator.translate("Pagination", Map.of()));
        assertEquals(2, requests.size());
    }

    @Test
    void 프롬프트에_합니다_어미_문구가_없다() {
        assertTrue(!OllamaTranslator.SYSTEM_PROMPT.contains("합니다"));
    }

    @Test
    void 원문에_없는_키릴_문자가_섞이면_한_번_다시_시도하고_그래도_섞이면_실패한다() {
        responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"읽기 전용 операции 입니다\"}}";
        OllamaTranslator translator = new OllamaTranslator(client());

        assertThrows(TranslationException.class, () -> translator.translate("Read-only operations", Map.of()));
        assertEquals(2, requests.size());
    }

    @Test
    void 원문에도_있는_문자는_문제로_보지_않는다() {
        assertTrue(!OllamaTranslator.hasForeignScript("漢字 test", "한자 漢字 테스트"));
    }

    @Test
    void 첫_시도가_비정상이면_강화된_지시로_다시_시도해_성공한다() {
        firstResponseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"읽기 전용 операции 입니다\"}}";
        responseBody = "{\"message\":{\"role\":\"assistant\",\"content\":\"읽기 전용 작업입니다\"}}";
        OllamaTranslator translator = new OllamaTranslator(client());

        String result = translator.translate("Read-only operations", Map.of());

        assertEquals("읽기 전용 작업입니다", result);
        assertEquals(2, requests.size());
        assertTrue(!requests.get(0).contains("Hangul"));
        assertTrue(requests.get(1).contains("Hangul"));
    }

    @Test
    void 번역_요청에는_최대_출력_길이가_들어간다() {
        OllamaTranslator translator = new OllamaTranslator(client());

        translator.translate("Hello", Map.of());

        assertTrue(requests.get(0).contains("\"num_predict\":256"));
    }

    @Test
    void 출력이_길이_제한에_걸리면_강화된_지시로_다시_시도해_성공한다() {
        firstResponseBody = "{\"done_reason\":\"length\",\"message\":{\"role\":\"assistant\",\"content\":\"반복 반복 반복\"}}";
        responseBody = "{\"done_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\"정상 번역입니다\"}}";
        OllamaTranslator translator = new OllamaTranslator(client());

        String result = translator.translate("A normal sentence.", Map.of());

        assertEquals("정상 번역입니다", result);
        assertEquals(2, requests.size());
        assertTrue(requests.get(1).contains("Hangul"));
    }

    @Test
    void 두_번_모두_길이_제한에_걸리면_실패한다() {
        responseBody = "{\"done_reason\":\"length\",\"message\":{\"role\":\"assistant\",\"content\":\"반복 반복 반복\"}}";
        OllamaTranslator translator = new OllamaTranslator(client());

        assertThrows(TranslationException.class, () -> translator.translate("A normal sentence.", Map.of()));
        assertEquals(2, requests.size());
    }

    @Test
    void 요약_요청에는_출력_길이_제한을_걸지_않는다() {
        OllamaSummarizer summarizer = new OllamaSummarizer(client(), 4000);

        summarizer.summarize(List.of("문단1"), SummaryLength.SHORT);

        assertTrue(!requests.get(0).contains("num_predict"));
    }
}
