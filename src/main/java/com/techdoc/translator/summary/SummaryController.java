package com.techdoc.translator.summary;

import com.techdoc.translator.document.DocumentResponse;
import com.techdoc.translator.document.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents/{id}")
public class SummaryController {

    private final SummaryService summaryService;
    private final DocumentService documentService;

    public SummaryController(SummaryService summaryService, DocumentService documentService) {
        this.summaryService = summaryService;
        this.documentService = documentService;
    }

    @PostMapping("/summarize")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse summarize(@PathVariable Long id, @Valid @RequestBody SummarizeRequest request) {
        summaryService.start(id, request.length());
        return documentService.get(id);
    }

    @GetMapping("/summary")
    public SummaryResponse summary(@PathVariable Long id) {
        return summaryService.get(id);
    }
}
