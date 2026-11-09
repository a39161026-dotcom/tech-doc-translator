package com.techdoc.translator.segment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SegmentSplitterTest {

    @Test
    void 빈_줄로_문단을_나눈다() {
        List<String> result = SegmentSplitter.split("First paragraph.\nstill first.\n\nSecond paragraph.");
        assertEquals(List.of("First paragraph.\nstill first.", "Second paragraph."), result);
    }

    @Test
    void 연속된_빈_줄과_앞뒤_공백은_무시한다() {
        List<String> result = SegmentSplitter.split("\n\n  A  \n\n\n\nB\n\n");
        assertEquals(List.of("A", "B"), result);
    }

    @Test
    void 코드_블록_안의_빈_줄은_나누지_않는다() {
        String text = "Intro\n\n```java\nint a = 1;\n\nint b = 2;\n```\n\nOutro";
        List<String> result = SegmentSplitter.split(text);
        assertEquals(3, result.size());
        assertTrue(result.get(1).contains("int a = 1;\n\nint b = 2;"));
    }

    @Test
    void CRLF도_처리한다() {
        assertEquals(List.of("A", "B"), SegmentSplitter.split("A\r\n\r\nB"));
    }

    @Test
    void 빈_입력은_빈_목록() {
        assertTrue(SegmentSplitter.split("  \n ").isEmpty());
        assertTrue(SegmentSplitter.split(null).isEmpty());
    }
}
