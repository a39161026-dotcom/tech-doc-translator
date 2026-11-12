package com.techdoc.translator.summary;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.document.Document;
import com.techdoc.translator.document.DocumentRepository;
import com.techdoc.translator.document.DocumentStatus;
import com.techdoc.translator.document.SummaryLength;
import com.techdoc.translator.document.SummaryStatus;
import com.techdoc.translator.segment.Segment;
import com.techdoc.translator.segment.SegmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final DocumentRepository documentRepository;
    private final SegmentRepository segmentRepository;
    private final Summarizer summarizer;
    private final TaskExecutor translationExecutor;

    public SummaryService(DocumentRepository documentRepository,
                          SegmentRepository segmentRepository,
                          Summarizer summarizer,
                          TaskExecutor translationExecutor) {
        this.documentRepository = documentRepository;
        this.segmentRepository = segmentRepository;
        this.summarizer = summarizer;
        this.translationExecutor = translationExecutor;
    }

    /** 요약 시작. 번역이 끝난(DONE) 문서만 가능하고, 다시 요약하면 덮어쓴다. */
    public void start(Long documentId, SummaryLength length) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."));

        if (document.getStatus() != DocumentStatus.DONE) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_READY", "번역이 완료된 문서만 요약할 수 있습니다.");
        }
        if (document.getSummaryStatus() == SummaryStatus.SUMMARIZING) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_SUMMARIZING", "이미 요약 중입니다.");
        }

        document.startSummary(length);
        documentRepository.save(document);

        translationExecutor.execute(() -> run(documentId, length));
    }

    void run(Long documentId, SummaryLength length) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return;
        }
        try {
            List<String> paragraphs = segmentRepository.findByDocumentIdOrderBySeq(documentId).stream()
                    .map(Segment::getTranslatedText)
                    .filter(Objects::nonNull)
                    .toList();
            document.completeSummary(summarizer.summarize(paragraphs, length));
        } catch (RuntimeException e) {
            log.warn("요약 실패 (document={}): {}", documentId, e.getMessage());
            document.failSummary();
        }
        documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public SummaryResponse get(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."));
        return new SummaryResponse(document.getSummaryStatus(), document.getSummaryLength(), document.getSummary());
    }
}
