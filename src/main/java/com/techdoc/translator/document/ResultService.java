package com.techdoc.translator.document;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.segment.Segment;
import com.techdoc.translator.segment.SegmentRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 결과 파일을 만든다.
 * - type: full(번역 전문), summary(요약본)
 * - format: md, txt, docx (pdf는 한글 폰트 처리 후 추가)
 */
@Service
public class ResultService {

    private static final MediaType DOCX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

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
    public ResultFile build(Long documentId, String type, String format) {
        String kind = type == null ? "full" : type.toLowerCase(Locale.ROOT);
        if (!kind.equals("full") && !kind.equals("summary")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TYPE", "type은 full 또는 summary만 가능합니다.");
        }
        String ext = format == null ? "md" : format.toLowerCase(Locale.ROOT);
        MediaType mediaType = switch (ext) {
            case "md" -> MediaType.parseMediaType("text/markdown;charset=UTF-8");
            case "txt" -> MediaType.parseMediaType("text/plain;charset=UTF-8");
            case "docx" -> DOCX;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FORMAT",
                    "지원하지 않는 형식입니다. (md, txt, docx만 가능)");
        };

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."));

        String body;
        String label;
        if (kind.equals("summary")) {
            if (document.getSummaryStatus() != SummaryStatus.DONE) {
                throw new ApiException(HttpStatus.CONFLICT, "SUMMARY_NOT_READY", "요약이 완료된 문서만 내려받을 수 있습니다.");
            }
            body = document.getSummary();
            label = "요약";
        } else {
            if (document.getStatus() != DocumentStatus.DONE) {
                throw new ApiException(HttpStatus.CONFLICT, "NOT_READY", "번역이 완료된 문서만 내려받을 수 있습니다.");
            }
            List<Segment> segments = segmentRepository.findByDocumentIdOrderBySeq(documentId);
            body = segments.stream().map(Segment::getTranslatedText).collect(Collectors.joining("\n\n"));
            label = "번역";
        }

        byte[] bytes = ext.equals("docx")
                ? toDocx(document.getTitle() + " (" + label + ")", body)
                : (body + "\n").getBytes(StandardCharsets.UTF_8);

        document.setResultFilePath(save(documentId + "-" + kind, ext, bytes));

        return new ResultFile(safeName(document.getTitle()) + "_" + label + "." + ext, mediaType, bytes);
    }

    private byte[] toDocx(String title, String body) {
        try (XWPFDocument docx = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFRun heading = docx.createParagraph().createRun();
            useKoreanFont(heading);
            heading.setBold(true);
            heading.setFontSize(16);
            heading.setText(title);

            for (String block : body.split("\\n\\s*\\n")) {
                String text = block.trim();
                if (text.isEmpty()) {
                    continue;
                }
                XWPFParagraph paragraph = docx.createParagraph();
                paragraph.setSpacingAfter(200);
                XWPFRun run = paragraph.createRun();
                useKoreanFont(run);
                String[] lines = text.split("\\R");
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) {
                        run.addBreak();
                    }
                    run.setText(lines[i]);
                }
            }
            docx.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_BUILD_FAILED", "문서 파일을 만들지 못했습니다.");
        }
    }

    private static void useKoreanFont(XWPFRun run) {
        run.setFontFamily("맑은 고딕");
        run.setFontFamily("맑은 고딕", XWPFRun.FontCharRange.eastAsia);
    }

    private String save(String name, String ext, byte[] bytes) {
        try {
            Files.createDirectories(resultDir);
            Path target = resultDir.resolve(name + "." + ext);
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
