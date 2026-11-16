package com.techdoc.translator.translation;

import com.techdoc.translator.llm.LlmException;
import com.techdoc.translator.llm.OllamaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Ollama 로컬 LLM 번역기. 한 문단씩 번역하고, 용어집은 프롬프트로 전달한다.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama")
public class OllamaTranslator implements Translator {

    static final String SYSTEM_PROMPT = """
            You are a professional translator of IT technical documents (English to Korean).
            Rules:
            - Output only the Korean translation. No explanations, no notes.
            - Keep code, commands, file paths, URLs, and identifiers exactly as they are.
            - Keep Markdown formatting (headings, lists, backticks) unchanged.
            - Use natural, concise Korean typical of developer documentation.
            - Use a polite, formal tone consistently.
            - Do not leave English words in the middle of Korean sentences, except identifiers,
              class names, annotations and widely used terms (e.g. Spring Boot, DTO, fetch join).
            """;

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");

    private final OllamaClient client;

    public OllamaTranslator(OllamaClient client) {
        this.client = client;
    }

    @Override
    public String translate(String sourceText, Map<String, String> glossary) {
        String trimmed = sourceText.strip();
        // 코드 블록은 LLM에 보내지 않고 그대로 둔다 (식별자가 번역되는 것을 막는다)
        if (trimmed.startsWith("```")) {
            return sourceText;
        }
        // 제목의 '#' 표시는 보존하고 제목 글자만 번역한다
        Matcher heading = HEADING.matcher(trimmed);
        if (heading.matches()) {
            return heading.group(1) + " " + translateText(heading.group(2), glossary);
        }
        return translateText(sourceText, glossary);
    }

    private String translateText(String text, Map<String, String> glossary) {
        try {
            String prompt = buildUserPrompt(text, glossary);
            String result = client.chat(SYSTEM_PROMPT, prompt);
            if (isRunaway(text, result)) {
                // 소형 모델이 내용을 지어내며 길게 답하는 경우가 있어 한 번만 다시 시도한다
                result = client.chat(SYSTEM_PROMPT, prompt);
                if (isRunaway(text, result)) {
                    throw new TranslationException("번역 결과가 원문에 비해 비정상적으로 깁니다.");
                }
            }
            return result;
        } catch (LlmException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    /** 결과가 원문의 3배(최소 80자)를 넘으면 모델이 내용을 지어낸 것으로 본다. */
    static boolean isRunaway(String source, String result) {
        return result.length() > Math.max(80, source.length() * 3);
    }

    static String buildUserPrompt(String sourceText, Map<String, String> glossary) {
        String lower = sourceText.toLowerCase();
        // 원문에 실제로 나오는 용어만 보낸다 (프롬프트를 짧게 유지해 속도와 안정성을 높인다)
        Map<String, String> relevant = glossary.entrySet().stream()
                .filter(e -> lower.contains(e.getKey().toLowerCase()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (relevant.isEmpty()) {
            return sourceText;
        }
        String terms = relevant.entrySet().stream()
                .map(e -> "- " + e.getKey() + " => " + e.getValue())
                .collect(Collectors.joining("\n"));
        return "Glossary (always translate these terms as given):\n" + terms
                + "\n\nText to translate:\n" + sourceText;
    }
}
