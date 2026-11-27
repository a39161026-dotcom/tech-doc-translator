package com.techdoc.translator.translation;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.document.Document;
import com.techdoc.translator.document.DocumentAccess;
import com.techdoc.translator.document.DocumentRepository;
import com.techdoc.translator.document.DocumentStatus;
import com.techdoc.translator.document.SummaryLength;
import com.techdoc.translator.segment.Segment;
import com.techdoc.translator.segment.SegmentRepository;
import com.techdoc.translator.segment.SegmentStatus;
import com.techdoc.translator.term.Term;
import com.techdoc.translator.summary.SummaryService;
import com.techdoc.translator.term.TermRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    private final DocumentRepository documentRepository;
    private final SegmentRepository segmentRepository;
    private final TermRepository termRepository;
    private final Translator translator;
    private final TaskExecutor translationExecutor;
    private final DocumentAccess documentAccess;
    private final SummaryService summaryService;

    @Value("${app.translation.default-glossary:true}")
    private boolean useDefaultGlossary;

    public TranslationService(DocumentRepository documentRepository,
                              SegmentRepository segmentRepository,
                              TermRepository termRepository,
                              Translator translator,
                              TaskExecutor translationExecutor,
                              DocumentAccess documentAccess,
                              SummaryService summaryService) {
        this.documentRepository = documentRepository;
        this.segmentRepository = segmentRepository;
        this.termRepository = termRepository;
        this.translator = translator;
        this.translationExecutor = translationExecutor;
        this.documentAccess = documentAccess;
        this.summaryService = summaryService;
    }

    /** 번역 시작. UPLOADED 상태의 문서만 가능하다. */
    public void start(Long documentId) {
        start(documentId, null);
    }

    /** 번역 시작. autoSummary를 주면 번역이 모두 성공한 뒤 그 길이로 요약까지 이어서 한다. */
    public void start(Long documentId, SummaryLength autoSummary) {
        launch(documentId, DocumentStatus.UPLOADED, autoSummary);
    }

    /** 실패한 문단만 다시 번역. FAILED 상태의 문서만 가능하다. */
    public void retry(Long documentId) {
        retry(documentId, null);
    }

    public void retry(Long documentId, SummaryLength autoSummary) {
        launch(documentId, DocumentStatus.FAILED, autoSummary);
    }

    private void launch(Long documentId, DocumentStatus requiredStatus, SummaryLength autoSummary) {
        documentAccess.requireOwner(documentId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."));

        if (document.getStatus() == DocumentStatus.TRANSLATING) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_TRANSLATING", "이미 번역 중입니다.");
        }
        if (document.getStatus() != requiredStatus) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "현재 상태(" + document.getStatus() + ")에서는 할 수 없습니다.");
        }

        document.changeStatus(DocumentStatus.TRANSLATING);
        documentRepository.save(document);

        translationExecutor.execute(() -> run(documentId, autoSummary));
    }

    /** 완료되지 않은(PENDING, FAILED) 문단을 순서대로 번역하고 문서 상태를 갱신한다. */
    void run(Long documentId) {
        run(documentId, null);
    }

    void run(Long documentId, SummaryLength autoSummary) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return;
        }

        boolean anyFailed = false;
        try {
            Map<String, String> glossary = loadGlossary(documentId);
            List<Segment> segments = segmentRepository.findByDocumentIdOrderBySeq(documentId);
            for (Segment segment : segments) {
                if (segment.getStatus() == SegmentStatus.DONE) {
                    continue;
                }
                try {
                    String translated = translator.translate(segment.getSourceText(), glossary);
                    segment.complete(translated);
                } catch (RuntimeException e) {
                    log.warn("문단 번역 실패 (document={}, seq={}): {}", documentId, segment.getSeq(), e.getMessage());
                    segment.fail();
                    anyFailed = true;
                }
                segmentRepository.save(segment);
            }
        } catch (RuntimeException e) {
            log.error("번역 작업 중 오류 (document={})", documentId, e);
            anyFailed = true;
        }

        document.changeStatus(anyFailed ? DocumentStatus.FAILED : DocumentStatus.DONE);
        // 번역 완료와 요약 시작을 한 번에 저장해서, 프론트가 조회할 때 "번역 끝, 요약 안 함" 상태가 잠깐 보이지 않게 한다.
        boolean chainSummary = !anyFailed && autoSummary != null;
        if (chainSummary) {
            document.startSummary(autoSummary);
        }
        documentRepository.save(document);
        if (chainSummary) {
            summaryService.runStarted(documentId, autoSummary);
        }
    }

    /** 문서 주인의 용어집을 (원어 -> 번역어) 맵으로 만든다. */
    private Map<String, String> loadGlossary(Long documentId) {
        Long memberId = documentRepository.findMemberId(documentId);
        Map<String, String> glossary = new LinkedHashMap<>();
        if (memberId != null) {
            for (Term term : termRepository.findByMemberId(memberId)) {
                glossary.put(term.getSourceTerm(), term.getTargetTerm());
            }
        }
        return useDefaultGlossary ? DefaultGlossary.merge(glossary) : glossary;
    }
}
