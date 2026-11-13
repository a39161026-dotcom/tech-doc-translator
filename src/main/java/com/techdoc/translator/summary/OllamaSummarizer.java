package com.techdoc.translator.summary;

import com.techdoc.translator.document.SummaryLength;
import com.techdoc.translator.llm.LlmException;
import com.techdoc.translator.llm.OllamaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Ollama 로컬 LLM 요약기. 한 번에 넣기엔 긴 문서는 조각별로 요약한 뒤,
 * 그 요약들을 합쳐 다시 요약한다(2단계).
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama")
public class OllamaSummarizer implements Summarizer {

    private final OllamaClient client;
    private final int chunkChars;

    public OllamaSummarizer(OllamaClient client,
                            @Value("${llm.summary-chunk-chars:4000}") int chunkChars) {
        this.client = client;
        this.chunkChars = chunkChars;
    }

    @Override
    public String summarize(List<String> translatedParagraphs, SummaryLength length) {
        if (translatedParagraphs.isEmpty()) {
            throw new SummaryException("요약할 내용이 없습니다.");
        }
        try {
            List<String> chunks = chunk(translatedParagraphs);
            if (chunks.size() == 1) {
                return client.chat(systemPrompt(length), chunks.get(0));
            }
            List<String> partials = new ArrayList<>();
            for (String chunk : chunks) {
                partials.add(client.chat(systemPrompt(SummaryLength.MEDIUM), chunk));
            }
            return client.chat(systemPrompt(length), String.join("\n\n", partials));
        } catch (LlmException e) {
            throw new SummaryException(e.getMessage(), e);
        }
    }

    List<String> chunk(List<String> paragraphs) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String p : paragraphs) {
            if (current.length() > 0 && current.length() + p.length() > chunkChars) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(p);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    static String systemPrompt(SummaryLength length) {
        String size = switch (length) {
            case SHORT -> "in 2-3 sentences";
            case MEDIUM -> "in one short paragraph (about 5 sentences)";
            case LONG -> "in 2-3 paragraphs, keeping the key points and steps";
        };
        return "You summarize Korean IT technical documents. Summarize the text " + size
                + ". Write in Korean. Output only the summary, no preface.";
    }
}
