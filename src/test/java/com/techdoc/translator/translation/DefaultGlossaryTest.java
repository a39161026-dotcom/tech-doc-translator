package com.techdoc.translator.translation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultGlossaryTest {

    @Test
    void 회원_용어집이_없으면_기본_용어가_들어간다() {
        Map<String, String> merged = DefaultGlossary.merge(Map.of());
        assertEquals("작업", merged.get("operations"));
        assertEquals("의존성 주입", merged.get("dependency injection"));
        assertEquals("전파", merged.get("propagation"));
        assertEquals("메서드", merged.get("method"));
    }

    @Test
    void 같은_원어는_대소문자_상관없이_회원_용어가_이긴다() {
        Map<String, String> merged = DefaultGlossary.merge(Map.of("Operations", "연산", "bean", "빈"));
        assertEquals("연산", merged.get("Operations"));
        assertFalse(merged.containsKey("operations"));
        assertEquals("빈", merged.get("bean"));
        assertTrue(merged.containsKey("request"));
    }
}
