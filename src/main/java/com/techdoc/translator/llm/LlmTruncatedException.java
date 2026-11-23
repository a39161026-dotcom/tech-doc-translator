package com.techdoc.translator.llm;

/** 모델이 최대 출력 길이까지 써도 끝내지 못했을 때 던진다. 같은 말을 반복하거나 다른 언어로 빠진 경우가 많다. */
public class LlmTruncatedException extends LlmException {

    public LlmTruncatedException(String message) {
        super(message);
    }
}
