package com.techdoc.translator.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
