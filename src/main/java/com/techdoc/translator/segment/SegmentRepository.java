package com.techdoc.translator.segment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SegmentRepository extends JpaRepository<Segment, Long> {
    List<Segment> findByDocumentIdOrderBySeq(Long documentId);
    List<Segment> findByDocumentIdAndStatus(Long documentId, SegmentStatus status);
}
