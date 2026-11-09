package com.techdoc.translator.document;

import com.techdoc.translator.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String title;

    private String originalFilename;
    private String fileType;
    private String filePath;       // 파일은 저장소에 두고 경로만 저장
    private String resultFilePath; // 번역 결과 파일 경로

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Document() {}

    public Document(Member member, String title, String originalFilename, String fileType, String filePath) {
        this.member = member;
        this.title = title;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.filePath = filePath;
    }

    public void changeStatus(DocumentStatus status) { this.status = status; }
    public void setResultFilePath(String resultFilePath) { this.resultFilePath = resultFilePath; }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public String getTitle() { return title; }
    public String getOriginalFilename() { return originalFilename; }
    public String getFileType() { return fileType; }
    public String getFilePath() { return filePath; }
    public String getResultFilePath() { return resultFilePath; }
    public DocumentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
