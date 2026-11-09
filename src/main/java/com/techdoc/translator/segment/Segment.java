package com.techdoc.translator.segment;

import com.techdoc.translator.document.Document;
import jakarta.persistence.*;

@Entity
@Table(name = "segment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "seq"}))
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(nullable = false)
    private int seq;

    @Lob
    @Column(nullable = false)
    private String sourceText;

    @Lob
    private String translatedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentStatus status = SegmentStatus.PENDING;

    protected Segment() {}

    public Segment(Document document, int seq, String sourceText) {
        this.document = document;
        this.seq = seq;
        this.sourceText = sourceText;
    }

    public void complete(String translatedText) {
        this.translatedText = translatedText;
        this.status = SegmentStatus.DONE;
    }

    public void fail() { this.status = SegmentStatus.FAILED; }

    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public int getSeq() { return seq; }
    public String getSourceText() { return sourceText; }
    public String getTranslatedText() { return translatedText; }
    public SegmentStatus getStatus() { return status; }
}
