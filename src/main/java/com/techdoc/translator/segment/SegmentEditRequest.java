package com.techdoc.translator.segment;

import jakarta.validation.constraints.NotBlank;

public record SegmentEditRequest(
        @NotBlank(message = "번역문(translatedText)을 입력해 주세요.") String translatedText) {
}
