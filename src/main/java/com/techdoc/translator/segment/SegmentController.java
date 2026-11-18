package com.techdoc.translator.segment;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/segments")
public class SegmentController {

    private final SegmentService segmentService;

    public SegmentController(SegmentService segmentService) {
        this.segmentService = segmentService;
    }

    @PatchMapping("/{id}")
    public SegmentResponse edit(@PathVariable Long id, @Valid @RequestBody SegmentEditRequest request) {
        return segmentService.edit(id, request.translatedText());
    }
}
