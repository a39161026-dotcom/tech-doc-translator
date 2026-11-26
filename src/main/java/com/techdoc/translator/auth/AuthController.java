package com.techdoc.translator.auth;

import com.techdoc.translator.member.Member;
import com.techdoc.translator.member.MemberResponse;
import com.techdoc.translator.member.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthController(MemberService memberService, JwtService jwtService, GoogleTokenVerifier googleTokenVerifier) {
        this.memberService = memberService;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Member member = memberService.authenticate(request.email(), request.password());
        return new LoginResponse(jwtService.issue(member), MemberResponse.of(member));
    }

    /** 프론트가 구글 팝업에서 받은 ID 토큰을 보내면, 구글에 확인한 뒤 우리 JWT를 발급한다. */
    @PostMapping("/google")
    public LoginResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleUser user = googleTokenVerifier.verify(request.idToken());
        Member member = memberService.findOrCreateSocial("GOOGLE", user.sub(), user.email(), user.name());
        return new LoginResponse(jwtService.issue(member), MemberResponse.of(member));
    }

    /** 토큰은 서버에 저장하지 않으므로 프론트가 토큰을 지우면 로그아웃이다. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
    }
}
