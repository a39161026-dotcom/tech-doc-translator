package com.techdoc.translator.auth;

/** 구글 ID 토큰을 확인해 사용자 정보를 돌려준다. 실패하면 ApiException(401). */
public interface GoogleTokenVerifier {

    GoogleUser verify(String idToken);

    record GoogleUser(String sub, String email, String name) {
    }
}
