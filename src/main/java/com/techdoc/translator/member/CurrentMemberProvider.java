package com.techdoc.translator.member;

import com.techdoc.translator.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 현재 사용자. 로그인(F1) 구현 전까지는 임시 회원(dev@local)을 돌려준다.
 * 로그인을 만들면 이 클래스만 바꾸면 된다.
 */
@Component
public class CurrentMemberProvider {

    private final MemberRepository memberRepository;

    public CurrentMemberProvider(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member get() {
        return memberRepository.findByEmail(DevDataInitializer.DEV_EMAIL)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다."));
    }
}
