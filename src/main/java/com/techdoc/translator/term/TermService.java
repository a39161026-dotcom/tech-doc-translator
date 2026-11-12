package com.techdoc.translator.term;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.member.CurrentMemberProvider;
import com.techdoc.translator.member.Member;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TermService {

    private final TermRepository termRepository;
    private final CurrentMemberProvider currentMember;

    public TermService(TermRepository termRepository, CurrentMemberProvider currentMember) {
        this.termRepository = termRepository;
        this.currentMember = currentMember;
    }

    @Transactional(readOnly = true)
    public List<TermResponse> list() {
        Member member = currentMember.get();
        return termRepository.findByMemberIdOrderByIdDesc(member.getId()).stream()
                .map(TermResponse::of)
                .toList();
    }

    @Transactional
    public TermResponse create(TermRequest request) {
        Member member = currentMember.get();
        String source = request.sourceTerm().trim();

        if (termRepository.findByMemberIdAndSourceTermIgnoreCase(member.getId(), source).isPresent()) {
            throw duplicated(source);
        }
        Term term = termRepository.save(
                new Term(member, source, request.targetTerm().trim(), normalizeNote(request.note())));
        return TermResponse.of(term);
    }

    @Transactional
    public TermResponse update(Long id, TermRequest request) {
        Member member = currentMember.get();
        Term term = find(id, member);
        String source = request.sourceTerm().trim();

        termRepository.findByMemberIdAndSourceTermIgnoreCase(member.getId(), source)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw duplicated(source); });

        term.update(source, request.targetTerm().trim(), normalizeNote(request.note()));
        return TermResponse.of(term);
    }

    @Transactional
    public void delete(Long id) {
        Member member = currentMember.get();
        termRepository.delete(find(id, member));
    }

    private Term find(Long id, Member member) {
        return termRepository.findByIdAndMemberId(id, member.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TERM_NOT_FOUND", "용어를 찾을 수 없습니다."));
    }

    private ApiException duplicated(String source) {
        return new ApiException(HttpStatus.CONFLICT, "TERM_DUPLICATED", "이미 등록된 용어입니다: " + source);
    }

    private String normalizeNote(String note) {
        return note == null ? null : note.trim();
    }
}
