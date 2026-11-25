package com.techdoc.translator.translation;

import com.techdoc.translator.llm.LlmException;
import com.techdoc.translator.llm.LlmTruncatedException;
import com.techdoc.translator.llm.OllamaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
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

    public static final String SYSTEM_PROMPT = """
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

    /**
     * 한 문단에 문장이 여러 개면 문장별로 따로 번역해 이어 붙인다.
     * 7b 모델은 긴 입력에서 다른 언어를 섞거나 끝나지 않는 출력을 내는 경우가 있어, 입력을 짧게 나눠 안정성을 높인다.
     */
    private String translateText(String text, Map<String, String> glossary) {
        List<String> sentences = splitSentences(text);
        if (sentences.size() < 2) {
            return translateOne(text, glossary);
        }
        return sentences.stream()
                .map(sentence -> translateOne(sentence, glossary))
                .collect(Collectors.joining(" "));
    }

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?])(?<!e\\.g\\.)(?<!i\\.e\\.)(?<!etc\\.)(?<!vs\\.)\\s+(?=[A-Z])");

    /** 줄바꿈이 없는 문단만 문장 단위로 나눈다. 줄바꿈이 있으면(목록 등) 나누지 않는다. */
    public static List<String> splitSentences(String text) {
        String trimmed = text.strip();
        if (trimmed.contains("\n")) {
            return List.of(trimmed);
        }
        return Arrays.asList(SENTENCE_BOUNDARY.split(trimmed));
    }

    private String translateOne(String text, Map<String, String> glossary) {
        try {
            String prompt = buildUserPrompt(text, glossary);
            int maxTokens = maxTokensFor(text);
            String result = null;
            try {
                result = client.chat(SYSTEM_PROMPT, prompt, maxTokens);
            } catch (LlmTruncatedException e) {
                // 끝나지 않는 출력: 아래에서 강화 지시로 한 번 다시 시도한다
            }
            if (result == null || isSuspicious(text, result)) {
                // 같은 프롬프트로는 같은 결과가 나오기 쉬우므로, 지시를 강화해서 한 번만 다시 시도한다
                try {
                    result = client.chat(SYSTEM_PROMPT + RETRY_NOTE, prompt, maxTokens);
                } catch (LlmTruncatedException e) {
                    throw new TranslationException("번역이 끝나지 않아 중단했습니다 (출력이 너무 길어짐).");
                }
                if (isSuspicious(text, result)) {
                    throw new TranslationException("번역 결과가 비정상적입니다 (과도하게 길거나 다른 언어 문자가 섞임).");
                }
            }
            return result;
        } catch (LlmException e) {
            throw new TranslationException(e.getMessage(), e);
        }
    }

    /**
     * 번역 한 번에 허용하는 최대 출력 토큰 수. 원문 글자 수의 3배(최소 256, 최대 2048).
     * 모델이 끝나지 않는 출력을 만들 때 읽기 시간 제한(300초)까지 기다리지 않기 위한 안전장치다.
     */
    static int maxTokensFor(String text) {
        return Math.min(2048, Math.max(256, text.length() * 3));
    }

    /** 첫 시도가 비정상일 때 시스템 프롬프트 뒤에 덧붙이는 강화 지시. */
    public static final String RETRY_NOTE = """

            IMPORTANT: Write the answer in Korean (Hangul) only. Never use Russian, Chinese or Japanese characters. Output only the translation of the given text and nothing else.
            """;

    private static final Pattern FOREIGN_SCRIPT = Pattern.compile("[\\u0400-\\u04FF\\u3040-\\u30FF\\u4E00-\\u9FFF]");

    static boolean isSuspicious(String source, String result) {
        return isRunaway(source, result) || hasForeignScript(source, result);
    }

    /** 원문에 없는 키릴 문자·일본어 가나·한자가 결과에 있으면 모델이 다른 언어를 섞은 것으로 본다. */
    public static boolean hasForeignScript(String source, String result) {
        return FOREIGN_SCRIPT.matcher(result).find() && !FOREIGN_SCRIPT.matcher(source).find();
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
