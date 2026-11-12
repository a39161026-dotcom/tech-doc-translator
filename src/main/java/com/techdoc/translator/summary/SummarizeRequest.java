package com.techdoc.translator.summary;

import com.techdoc.translator.document.SummaryLength;
import jakarta.validation.constraints.NotNull;

public record SummarizeRequest(
        @NotNull(message = "요약 길이(length)를 선택해 주세요.") SummaryLength length) {
}
