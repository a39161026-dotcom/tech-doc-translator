package com.techdoc.translator.summary;

import com.techdoc.translator.document.SummaryLength;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakeSummarizerTest {

    private final FakeSummarizer summarizer = new FakeSummarizer();
    private final List<String> paragraphs = List.of("A", "B", "C", "D", "E", "F");

    @Test
    void 길이에_따라_앞쪽_문단_수가_달라진다() {
        assertEquals("[요약] A", summarizer.summarize(paragraphs, SummaryLength.SHORT));
        assertEquals("[요약] A\n\nB\n\nC", summarizer.summarize(paragraphs, SummaryLength.MEDIUM));
        assertEquals("[요약] A\n\nB\n\nC\n\nD\n\nE", summarizer.summarize(paragraphs, SummaryLength.LONG));
    }

    @Test
    void 문단이_부족하면_있는_만큼만() {
        assertEquals("[요약] A\n\nB", summarizer.summarize(List.of("A", "B"), SummaryLength.LONG));
    }

    @Test
    void 비어_있으면_실패한다() {
        assertThrows(SummaryException.class, () -> summarizer.summarize(List.of(), SummaryLength.SHORT));
    }

    @Test
    void SUMMARY_FAIL이_있으면_실패한다() {
        assertThrows(SummaryException.class,
                () -> summarizer.summarize(List.of("ok", "SUMMARY_FAIL"), SummaryLength.SHORT));
    }
}
