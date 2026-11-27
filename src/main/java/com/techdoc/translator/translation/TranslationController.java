package com.techdoc.translator.translation;

import com.techdoc.translator.document.DocumentResponse;
import com.techdoc.translator.document.DocumentService;
import com.techdoc.translator.document.SummaryLength;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents/{id}")
public class TranslationController {

    private final TranslationService translationService;
    private final DocumentService documentService;

    public TranslationController(TranslationService translationService, DocumentService documentService) {
        this.translationService = translationService;
        this.documentService = documentService;
    }

    @PostMapping("/translate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse translate(@PathVariable Long id,
                                      @RequestParam(value = "summaryLength", required = false) SummaryLength summaryLength) {
        translationService.start(id, summaryLength);
        return documentService.get(id);
    }

    @PostMapping("/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse retry(@PathVariable Long id,
                                  @RequestParam(value = "summaryLength", required = false) SummaryLength summaryLength) {
        translationService.retry(id, summaryLength);
        return documentService.get(id);
    }
}
