package com.techdoc.translator.auth;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(@NotBlank String accessToken) {
}
