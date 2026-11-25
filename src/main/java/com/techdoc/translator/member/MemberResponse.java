package com.techdoc.translator.member;

public record MemberResponse(Long id, String email, String nickname) {

    public static MemberResponse of(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname());
    }
}
