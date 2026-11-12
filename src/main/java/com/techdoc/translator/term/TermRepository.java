package com.techdoc.translator.term;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    List<Term> findByMemberId(Long memberId);
    List<Term> findByMemberIdOrderByIdDesc(Long memberId);
    Optional<Term> findByIdAndMemberId(Long id, Long memberId);
    Optional<Term> findByMemberIdAndSourceTermIgnoreCase(Long memberId, String sourceTerm);
}
