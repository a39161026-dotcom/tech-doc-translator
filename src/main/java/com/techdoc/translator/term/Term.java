package com.techdoc.translator.term;

import com.techdoc.translator.member.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "term",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "sourceTerm"}))
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String sourceTerm;

    @Column(nullable = false)
    private String targetTerm;

    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Term() {}

    public Term(Member member, String sourceTerm, String targetTerm, String note) {
        this.member = member;
        this.sourceTerm = sourceTerm;
        this.targetTerm = targetTerm;
        this.note = note;
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public String getSourceTerm() { return sourceTerm; }
    public String getTargetTerm() { return targetTerm; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
