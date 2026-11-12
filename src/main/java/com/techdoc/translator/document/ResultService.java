package com.techdoc.translator.document;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.segment.Segment;
import com.techdoc.translator.segment.SegmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 번역이 끝난 문서를 파일로 만든다. 지금은 md, txt만 지원한다. (docx, pdf는 형식 확정 후)
 */
@Service
public class ResultService {

    private final DocumentRepository documentRepository;
    private final SegmentRepository segmentRepository;
    private final Path resultDir;

    public ResultService(DocumentRepository documentRepository,
                         SegmentRepository segmentRepository,
                         @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.segmentRepository = segmentRepository;
        this.resultDir = Paths.get(uploadDir, "results");
    }

    @Transactional
    public ResultFile build(Long documentId, String format) {
        String ext = format == null ? "md" : format.toLowerCase(Locale.ROOT);
        MediaType mediaType = switch (ext) {
            case "md" -> MediaType.parseMediaType("text/markdown;charset=UTF-8");
            case "txt" -> MediaType.parseMediaType("text/plain;charset=UTF-8");
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FORMAT",
                    "지원하지 않는 형식입니다. (md, txt만 가능)");
        };

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."));
        if (document.getStatus() != DocumentStatus.DONE) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_READY", "번역이 완료된 문서만 내려받을 수 있습니다.");
        }

        List<Segment> segments = segmentRepository.findByDocumentIdOrderBySeq(documentId);
        String text = segments.stream()
                .map(Segment::getTranslatedText)
                .collect(Collectors.joining("\n\n")) + "\n";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        document.setResultFilePath(save(documentId, ext, bytes));

        return new ResultFile(safeName(document.getTitle()) + "_번역." + ext, mediaType, bytes);
    }

    private String save(Long documentId, String ext, byte[] bytes) {
        try {
            Files.createDirectories(resultDir);
            Path target = resultDir.resolve(documentId + "." + ext);
            Files.write(target, bytes);
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_SAVE_FAILED", "결과 파일 저장에 실패했습니다.");
        }
    }

    private static String safeName(String title) {
        String name = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return name.isEmpty() ? "document" : name;
    }
}
