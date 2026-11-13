package com.techdoc.translator.summary;

import com.techdoc.translator.document.SummaryLength;

import java.util.List;

/**
 * 요약기. 지금은 FakeSummarizer, 이후 로컬 LLM 구현체로 교체한다.
 * 긴 문서는 문단별로 요약한 뒤 합치는 방식이 필요할 수 있다(구현체가 결정).
 */
public interface Summarizer {

    /**
     * @param translatedParagraphs 번역된 문단 목록 (문서 순서대로)
     * @param length               요약 길이
     * @return 요약문
     * @throws SummaryException 요약에 실패한 경우
     */
    String summarize(List<String> translatedParagraphs, SummaryLength length);
}
