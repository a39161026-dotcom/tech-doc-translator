package com.techdoc.translator.document;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.member.CurrentMemberProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 문서가 현재 회원의 것인지 확인한다.
 * 남의 문서는 "없는 문서"와 같은 404로 답해서 문서가 존재하는지도 알 수 없게 한다.
 */
@Component
public class DocumentAccess {

    private final DocumentRepository documentRepository;
    private final CurrentMemberProvider currentMember;

    public DocumentAccess(DocumentRepository documentRepository, CurrentMemberProvider currentMember) {
        this.documentRepository = documentRepository;
        this.currentMember = currentMember;
    }

    public void requireOwner(Long documentId) {
        Long ownerId = documentRepository.findMemberId(documentId);
        Long memberId = currentMember.get().getId();
        if (ownerId == null || !ownerId.equals(memberId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다.");
        }
    }
}
