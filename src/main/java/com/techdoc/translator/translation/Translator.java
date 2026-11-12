package com.techdoc.translator.translation;

import java.util.Map;

/**
 * 번역기. 지금은 FakeTranslator, 이후 로컬 LLM 구현체로 교체한다.
 */
public interface Translator {

    /**
     * @param sourceText 번역할 원문 한 문단
     * @param glossary   용어집 (원어 -> 번역어). 없으면 빈 맵
     * @return 번역된 문단
     * @throws TranslationException 번역에 실패한 경우
     */
    String translate(String sourceText, Map<String, String> glossary);
}
