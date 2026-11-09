package com.techdoc.translator.member;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 로그인(F1) 구현 전까지 쓰는 임시 회원. 로그인 구현 후 삭제한다.
 */
@Component
public class DevDataInitializer implements CommandLineRunner {

    public static final String DEV_EMAIL = "dev@local";

    private final MemberRepository memberRepository;

    public DevDataInitializer(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) {
        if (memberRepository.findByEmail(DEV_EMAIL).isEmpty()) {
            memberRepository.save(new Member(DEV_EMAIL, "dev-only", "dev"));
        }
    }
}
