package com.techdoc.translator.member;

import com.techdoc.translator.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class MemberService {

    /** BCrypt는 비밀번호를 72바이트까지만 쓴다. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public MemberResponse signup(SignupRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_LONG", "비밀번호가 너무 깁니다.");
        }
        String email = normalize(request.email());
        if (memberRepository.findByEmail(email).isPresent()) {
            throw emailTaken();
        }
        try {
            Member member = memberRepository.saveAndFlush(
                    new Member(email, passwordEncoder.encode(request.password()), request.nickname().trim()));
            return MemberResponse.of(member);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 이메일로 가입한 경우
            throw emailTaken();
        }
    }

    /** 이메일과 비밀번호가 맞으면 회원을, 아니면 401 예외를 던진다. 어느 쪽이 틀렸는지는 알려 주지 않는다. */
    public Member authenticate(String email, String password) {
        return memberRepository.findByEmail(normalize(email))
                .filter(member -> passwordEncoder.matches(password, member.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
    }
}
