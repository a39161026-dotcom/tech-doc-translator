package com.techdoc.translator.member;

import com.techdoc.translator.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

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

    /**
     * 소셜 로그인 회원을 찾거나 만든다. 제공자 계정이 이미 있으면 그 회원, 없고 같은 이메일 회원이 있으면 연결,
     * 둘 다 없으면 새로 만든다. 이메일은 제공자가 인증한 것만 받아야 한다(호출하는 쪽에서 확인).
     */
    public Member findOrCreateSocial(String provider, String providerId, String email, String name) {
        return memberRepository.findByProviderAndProviderId(provider, providerId).orElseGet(() -> {
            String normalized = normalize(email);
            Member member = memberRepository.findByEmail(normalized).orElse(null);
            if (member == null) {
                String nickname = (name == null || name.isBlank()) ? normalized.substring(0, normalized.indexOf('@')) : name.trim();
                // 비밀번호 로그인은 못 하도록 아무도 모르는 값을 넣는다.
                member = new Member(normalized, passwordEncoder.encode(UUID.randomUUID().toString()), nickname);
            }
            member.linkProvider(provider, providerId);
            return memberRepository.save(member);
        });
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
    }
}
