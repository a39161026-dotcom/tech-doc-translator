package com.techdoc.translator.translation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 개발용 가짜 번역기. 원문 앞에 "[번역] "을 붙인다.
 * 용어집이 있으면 원문 속 용어를 번역어로 바꾼다.
 * 실패 흐름을 시험하기 위해 원문에 특정 문구가 있으면 실패한다.
 * - "FAIL": 항상 실패
 * - "FAIL_ONCE": 처음 한 번만 실패 (재시도 시험용)
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeTranslator implements Translator {

    private final Set<String> alreadyFailedOnce = ConcurrentHashMap.newKeySet();

    @Override
    public String translate(String sourceText, Map<String, String> glossary) {
        if (sourceText.contains("FAIL_ONCE")) {
            if (alreadyFailedOnce.add(sourceText)) {
                throw new TranslationException("가짜 번역기: 첫 시도 실패");
            }
        } else if (sourceText.contains("FAIL")) {
            throw new TranslationException("가짜 번역기: 실패");
        }
        String text = sourceText;
        for (Map.Entry<String, String> entry : glossary.entrySet()) {
            text = Pattern.compile(Pattern.quote(entry.getKey()), Pattern.CASE_INSENSITIVE)
                    .matcher(text)
                    .replaceAll(Matcher.quoteReplacement(entry.getValue()));
        }
        return "[번역] " + text;
    }
}
