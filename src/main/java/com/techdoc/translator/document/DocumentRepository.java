package com.techdoc.translator.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    @Query("select d.member.id from Document d where d.id = :id")
    Long findMemberId(@Param("id") Long id);
}
