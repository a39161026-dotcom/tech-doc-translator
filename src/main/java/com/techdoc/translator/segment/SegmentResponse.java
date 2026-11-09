package com.techdoc.translator.segment;

public record SegmentResponse(
        Long id,
        int seq,
        String sourceText,
        String translatedText,
        SegmentStatus status) {

    static SegmentResponse of(Segment s) {
        return new SegmentResponse(s.getId(), s.getSeq(), s.getSourceText(),
                s.getTranslatedText(), s.getStatus());
    }
}
