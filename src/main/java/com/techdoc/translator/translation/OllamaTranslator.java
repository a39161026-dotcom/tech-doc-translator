package com.techdoc.translator.translation;

import com.techdoc.translator.llm.LlmException;
import com.techdoc.translator.llm.OllamaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
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
            """;

    private final OllamaClient client;

    public OllamaTranslator(OllamaClient client) {
        this.client = client;
    }

    @Override
    public String translate(String sourceText, Map<String, String> glossary) {
        try {
            return client.chat(SYSTEM_PROMPT, buildUserPrompt(sourceText, glossary));
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
