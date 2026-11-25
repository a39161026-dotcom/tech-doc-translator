package com.techdoc.translator.member;

import com.techdoc.translator.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 현재 사용자.
 * - app.auth.enabled=true: 요청의 JWT로 확인한 회원 (AuthInterceptor가 요청에 회원 id를 넣어 둔다)
 * - 꺼져 있으면(기본값): 임시 회원(dev@local)
 * 요청 스레드에서만 부를 수 있다 (번역·요약 같은 백그라운드 작업에서는 회원 id를 따로 넘긴다).
 */
@Component
public class CurrentMemberProvider {

    public static final String REQUEST_ATTR = "authMemberId";

    private final MemberRepository memberRepository;
    private final boolean authEnabled;

    public CurrentMemberProvider(MemberRepository memberRepository,
                                 @Value("${app.auth.enabled:false}") boolean authEnabled) {
        this.memberRepository = memberRepository;
        this.authEnabled = authEnabled;
    }

    public Member get() {
        if (!authEnabled) {
            return memberRepository.findByEmail(DevDataInitializer.DEV_EMAIL).orElseThrow(CurrentMemberProvider::loginRequired);
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw loginRequired();
        }
        Object memberId = attributes.getRequest().getAttribute(REQUEST_ATTR);
        if (!(memberId instanceof Long id)) {
            throw loginRequired();
        }
        return memberRepository.findById(id).orElseThrow(CurrentMemberProvider::loginRequired);
    }

    private static ApiException loginRequired() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다.");
    }
}
