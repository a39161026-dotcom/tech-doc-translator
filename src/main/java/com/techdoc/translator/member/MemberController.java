package com.techdoc.translator.member;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final CurrentMemberProvider currentMember;

    public MemberController(MemberService memberService, CurrentMemberProvider currentMember) {
        this.memberService = memberService;
        this.currentMember = currentMember;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request) {
        return memberService.signup(request);
    }

    /** 지금 로그인한 회원. 프론트가 저장해 둔 토큰이 아직 유효한지 확인할 때 쓴다. */
    @GetMapping("/me")
    public MemberResponse me() {
        return MemberResponse.of(currentMember.get());
    }
}
