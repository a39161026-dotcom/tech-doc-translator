package com.techdoc.translator.summary;

public class SummaryException extends RuntimeException {

    public SummaryException(String message) {
        super(message);
    }

    public SummaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
