package com.techdoc.translator.segment;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.document.Document;
import com.techdoc.translator.document.DocumentAccess;
import com.techdoc.translator.document.DocumentRepository;
import com.techdoc.translator.document.DocumentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SegmentService {

    private final SegmentRepository segmentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAccess documentAccess;

    public SegmentService(SegmentRepository segmentRepository, DocumentRepository documentRepository,
                          DocumentAccess documentAccess) {
        this.segmentRepository = segmentRepository;
        this.documentRepository = documentRepository;
        this.documentAccess = documentAccess;
    }

    /**
     * 문단의 번역문을 직접 고친다 (모델이 실패했거나 어색하게 번역한 문단 대응).
     * 번역이 끝난(DONE) 문서나 실패 문단이 있는(FAILED) 문서만 가능하다.
     * 고친 문단은 DONE이 되고, 실패·미번역 문단이 더 없으면 문서도 DONE이 된다.
     */
    @Transactional
    public SegmentResponse edit(Long segmentId, String translatedText) {
        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "문단을 찾을 수 없습니다."));
        Document document = segment.getDocument();
        documentAccess.requireOwner(document.getId());

        if (document.getStatus() == DocumentStatus.TRANSLATING) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_TRANSLATING", "번역 중에는 수정할 수 없습니다.");
        }
        if (document.getStatus() == DocumentStatus.UPLOADED) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_TRANSLATED", "번역이 끝난 뒤에 수정할 수 있습니다.");
        }

        segment.complete(translatedText);
        segmentRepository.save(segment);

        if (document.getStatus() == DocumentStatus.FAILED) {
            Long documentId = document.getId();
            long failed = segmentRepository.countByDocumentIdAndStatus(documentId, SegmentStatus.FAILED);
            long pending = segmentRepository.countByDocumentIdAndStatus(documentId, SegmentStatus.PENDING);
            if (failed == 0 && pending == 0) {
                document.changeStatus(DocumentStatus.DONE);
                documentRepository.save(document);
            }
        }
        return SegmentResponse.of(segment);
    }
}
