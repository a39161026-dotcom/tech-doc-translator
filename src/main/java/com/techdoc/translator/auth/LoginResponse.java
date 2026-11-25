package com.techdoc.translator.auth;

import com.techdoc.translator.member.MemberResponse;

public record LoginResponse(String token, MemberResponse member) {
}
