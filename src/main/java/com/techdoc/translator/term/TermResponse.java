package com.techdoc.translator.term;

public record TermResponse(Long id, String sourceTerm, String targetTerm, String note) {

    static TermResponse of(Term t) {
        return new TermResponse(t.getId(), t.getSourceTerm(), t.getTargetTerm(), t.getNote());
    }
}
