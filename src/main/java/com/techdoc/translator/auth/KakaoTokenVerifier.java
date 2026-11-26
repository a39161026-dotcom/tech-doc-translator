package com.techdoc.translator.auth;

/** 카카오 액세스 토큰을 확인해 사용자 정보를 돌려준다. 실패하면 ApiException(401). */
public interface KakaoTokenVerifier {

    KakaoUser verify(String accessToken);

    /** id는 카카오 회원번호. 닉네임은 동의하지 않았으면 null. */
    record KakaoUser(String id, String nickname) {
    }
}
