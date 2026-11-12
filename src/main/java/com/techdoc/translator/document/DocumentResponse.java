package com.techdoc.translator.document;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String title,
        String originalFilename,
        DocumentStatus status,
        int segmentCount,
        int doneCount,
        int failedCount,
        LocalDateTime createdAt) {

    static DocumentResponse of(Document d, int segmentCount, int doneCount, int failedCount) {
        return new DocumentResponse(d.getId(), d.getTitle(), d.getOriginalFilename(),
                d.getStatus(), segmentCount, doneCount, failedCount, d.getCreatedAt());
    }
}
