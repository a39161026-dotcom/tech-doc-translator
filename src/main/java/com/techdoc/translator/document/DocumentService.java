package com.techdoc.translator.document;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.member.CurrentMemberProvider;
import com.techdoc.translator.member.Member;
import com.techdoc.translator.segment.Segment;
import com.techdoc.translator.segment.SegmentRepository;
import com.techdoc.translator.segment.SegmentResponse;
import com.techdoc.translator.segment.SegmentSplitter;
import com.techdoc.translator.segment.SegmentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("txt", "md");

    private final DocumentRepository documentRepository;
    private final SegmentRepository segmentRepository;
    private final CurrentMemberProvider currentMember;
    private final Path uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           SegmentRepository segmentRepository,
                           CurrentMemberProvider currentMember,
                           @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.segmentRepository = segmentRepository;
        this.currentMember = currentMember;
        this.uploadDir = Paths.get(uploadDir);
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "파일이 비어 있습니다.");
        }

        String originalFilename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String extension = extensionOf(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_TYPE",
                    "지원하지 않는 파일 형식입니다. (txt, md만 가능)");
        }

        String text;
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "파일을 읽을 수 없습니다.");
        }
        text = new String(bytes, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }

        List<String> paragraphs = SegmentSplitter.split(text);
        if (paragraphs.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NO_CONTENT", "문서에 내용이 없습니다.");
        }

        String storedPath = store(bytes, extension);

        Member member = currentMember.get();

        String docTitle = (title == null || title.isBlank()) ? stripExtension(originalFilename) : title.trim();
        Document document = documentRepository.save(
                new Document(member, docTitle, originalFilename, extension, storedPath));

        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < paragraphs.size(); i++) {
            segments.add(new Segment(document, i + 1, paragraphs.get(i)));
        }
        segmentRepository.saveAll(segments);

        return toResponse(document);
    }

    /** 현재 회원의 문서 목록 (최신순). */
    @Transactional(readOnly = true)
    public List<DocumentResponse> list() {
        Long memberId = currentMember.get().getId();
        return documentRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<SegmentResponse> segments(Long id) {
        find(id);
        return segmentRepository.findByDocumentIdOrderBySeq(id).stream()
                .map(SegmentResponse::of)
                .toList();
    }

    /** 문서와 문단을 삭제한다. 번역·요약이 진행 중이면 삭제할 수 없다. */
    @Transactional
    public void delete(Long id) {
        Document document = find(id);
        if (document.getStatus() == DocumentStatus.TRANSLATING
                || document.getSummaryStatus() == SummaryStatus.SUMMARIZING) {
            throw new ApiException(HttpStatus.CONFLICT, "DOCUMENT_BUSY", "번역 또는 요약 중에는 삭제할 수 없습니다.");
        }
        String filePath = document.getFilePath();
        segmentRepository.deleteByDocumentId(id);
        documentRepository.delete(document);
        deleteStoredFile(filePath);
    }

    /** 업로드 원본 파일 삭제. 실패해도 문서 삭제는 막지 않는다. */
    private void deleteStoredFile(String filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // 파일이 남아도 DB 기록은 이미 지워졌으므로 무시한다
        }
    }

    private DocumentResponse toResponse(Document document) {
        Long id = document.getId();
        return DocumentResponse.of(document,
                (int) segmentRepository.countByDocumentId(id),
                (int) segmentRepository.countByDocumentIdAndStatus(id, SegmentStatus.DONE),
                (int) segmentRepository.countByDocumentIdAndStatus(id, SegmentStatus.FAILED));
    }

    private Document find(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."));
    }

    private String store(byte[] bytes, String extension) {
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(UUID.randomUUID() + "." + extension);
            Files.write(target, bytes);
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_SAVE_FAILED", "파일 저장에 실패했습니다.");
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? filename : filename.substring(0, dot);
    }
}
