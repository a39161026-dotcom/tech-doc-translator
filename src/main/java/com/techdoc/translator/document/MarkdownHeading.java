package com.techdoc.translator.document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 번역문 속 마크다운 제목 줄("# 제목", "## 제목" ...)을 알아본다. PDF·docx에서 #을 빼고 제목 서식으로 쓰기 위한 도우미. */
record MarkdownHeading(int level, String text) {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");

    /** 제목 줄이면 제목 정보를, 아니면 null을 돌려준다. */
    static MarkdownHeading parse(String line) {
        Matcher m = HEADING.matcher(line.strip());
        if (!m.matches()) {
            return null;
        }
        return new MarkdownHeading(m.group(1).length(), m.group(2));
    }

    /** 제목 단계별 글자 크기(pt). 1단계가 가장 크다. */
    int fontSize() {
        return switch (level) {
            case 1 -> 16;
            case 2 -> 14;
            default -> 12;
        };
    }
}
