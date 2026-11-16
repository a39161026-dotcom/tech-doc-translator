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
            - Write every sentence in the polite style ending with -합니다 / -입니다. Never mix in -다 style.
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
            return client.chat(SYSTEM_PROMPT, buildUserPrompt(text, glossary));
        } catch (LlmException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    static String buildUserPrompt(String sourceText, Map<String, String> glossary) {
        if (glossary.isEmpty()) {
            return sourceText;
        }
        String terms = glossary.entrySet().stream()
                .map(e -> "- " + e.getKey() + " => " + e.getValue())
                .collect(Collectors.joining("\n"));
        return "Glossary (always translate these terms as given):\n" + terms
                + "\n\nText to translate:\n" + sourceText;
    }
}
