package com.techdoc.translator.document;

import com.techdoc.translator.segment.SegmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "title", required = false) String title) {
        return documentService.upload(file, title);
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable Long id) {
        return documentService.get(id);
    }

    @GetMapping("/{id}/segments")
    public List<SegmentResponse> segments(@PathVariable Long id) {
        return documentService.segments(id);
    }
}
