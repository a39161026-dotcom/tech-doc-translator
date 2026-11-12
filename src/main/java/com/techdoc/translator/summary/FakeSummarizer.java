package com.techdoc.translator.summary;

import com.techdoc.translator.document.SummaryLength;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 개발용 가짜 요약기. 앞쪽 문단 N개를 이어 붙이고 "[요약] "을 붙인다.
 * SHORT=1개, MEDIUM=3개, LONG=5개. 문단에 "SUMMARY_FAIL"이 있으면 실패한다.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "fake", matchIfMissing = true)
public class FakeSummarizer implements Summarizer {

    @Override
    public String summarize(List<String> translatedParagraphs, SummaryLength length) {
        if (translatedParagraphs.isEmpty()) {
            throw new SummaryException("요약할 내용이 없습니다.");
        }
        if (translatedParagraphs.stream().anyMatch(p -> p.contains("SUMMARY_FAIL"))) {
            throw new SummaryException("가짜 요약기: 실패");
        }
        int count = switch (length) {
            case SHORT -> 1;
            case MEDIUM -> 3;
            case LONG -> 5;
        };
        return "[요약] " + String.join("\n\n", translatedParagraphs.subList(0, Math.min(count, translatedParagraphs.size())));
    }
}
