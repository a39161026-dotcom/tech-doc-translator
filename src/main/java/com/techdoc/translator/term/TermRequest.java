package com.techdoc.translator.term;

import jakarta.validation.constraints.NotBlank;

public record TermRequest(
        @NotBlank(message = "원어(sourceTerm)를 입력해 주세요.") String sourceTerm,
        @NotBlank(message = "번역어(targetTerm)를 입력해 주세요.") String targetTerm,
        String note) {
}
