package com.techdoc.translator.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownHeadingTest {

    @Test
    void 제목_줄은_단계와_글자를_뽑는다() {
        MarkdownHeading h1 = MarkdownHeading.parse("# 트랜잭션 관리");
        assertEquals(1, h1.level());
        assertEquals("트랜잭션 관리", h1.text());
        assertEquals(16, h1.fontSize());

        MarkdownHeading h2 = MarkdownHeading.parse("  ## 전파  ");
        assertEquals(2, h2.level());
        assertEquals("전파", h2.text());
        assertEquals(14, h2.fontSize());

        assertEquals(12, MarkdownHeading.parse("#### 깊은 제목").fontSize());
    }

    @Test
    void 제목이_아닌_줄은_null() {
        assertNull(MarkdownHeading.parse("본문입니다."));
        assertNull(MarkdownHeading.parse("#해시뿐"));
        assertNull(MarkdownHeading.parse("####### 일곱 개"));
        assertNull(MarkdownHeading.parse("# "));
    }

    @Test
    void PDF는_제목이_섞여_있어도_만들어진다() {
        byte[] pdf = PdfBuilder.build("문서 (번역)",
                "# 제목\n\n본문 첫 줄\n본문 둘째 줄\n\n## 소제목\n내용");
        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
