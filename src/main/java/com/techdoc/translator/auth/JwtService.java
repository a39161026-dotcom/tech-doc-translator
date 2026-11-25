package com.techdoc.translator.auth;

import com.techdoc.translator.common.ApiException;
import com.techdoc.translator.member.Member;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/** JWT 발급과 검증. 토큰의 subject는 회원 id다. */
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${app.auth.jwt-secret}") String secret,
                      @Value("${app.auth.token-hours:24}") long tokenHours) {
        // HS256은 키가 32바이트 이상이어야 한다
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(tokenHours);
    }

    public String issue(Member member) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 토큰이 올바르면 회원 id를, 아니면(위조·만료 등) 401 예외를 던진다. */
    public Long parseMemberId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "로그인이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.");
        }
    }
}
