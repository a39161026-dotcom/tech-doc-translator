package com.techdoc.translator.segment;

import java.util.ArrayList;
import java.util.List;

/**
 * 텍스트를 문단 단위로 나눈다.
 * 빈 줄을 기준으로 나누되, 마크다운 코드 블록(```) 안의 빈 줄은 나누지 않는다.
 */
public final class SegmentSplitter {

    private SegmentSplitter() {}

    public static List<String> split(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean inFence = false;

        for (String line : text.split("\\R", -1)) {
            if (line.trim().startsWith("```")) {
                inFence = !inFence;
                current.append(line).append('\n');
                continue;
            }
            if (!inFence && line.isBlank()) {
                flush(current, result);
                continue;
            }
            current.append(line).append('\n');
        }
        flush(current, result);
        return result;
    }

    private static void flush(StringBuilder current, List<String> result) {
        String paragraph = current.toString().trim();
        if (!paragraph.isEmpty()) {
            result.add(paragraph);
        }
        current.setLength(0);
    }
}
