package com.techdoc.translator.translation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakeTranslatorTest {

    private final FakeTranslator translator = new FakeTranslator();

    @Test
    void 원문_앞에_번역_표시를_붙인다() {
        assertEquals("[번역] Hello", translator.translate("Hello", Map.of()));
    }

    @Test
    void FAIL이_들어있으면_항상_실패한다() {
        assertThrows(TranslationException.class, () -> translator.translate("please FAIL", Map.of()));
        assertThrows(TranslationException.class, () -> translator.translate("please FAIL", Map.of()));
    }

    @Test
    void FAIL_ONCE는_처음만_실패하고_다음엔_성공한다() {
        String text = "retry me FAIL_ONCE";
        assertThrows(TranslationException.class, () -> translator.translate(text, Map.of()));
        assertEquals("[번역] " + text, translator.translate(text, Map.of()));
    }

    @Test
    void 용어집의_용어를_번역어로_바꾼다_대소문자_무시() {
        String result = translator.translate("Dependency Injection is key.",
                Map.of("dependency injection", "의존성 주입"));
        assertEquals("[번역] 의존성 주입 is key.", result);
    }
}
