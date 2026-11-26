package com.techdoc.translator.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.techdoc.translator.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;

/**
 * 구글 tokeninfo 엔드포인트로 ID 토큰을 확인한다. 서명과 만료는 구글이 검사하고,
 * 여기서는 우리 앱용 토큰인지(aud), 구글이 발급한 것인지(iss), 이메일이 인증됐는지 확인한다.
 */
@Component
public class GoogleTokenInfoVerifier implements GoogleTokenVerifier {

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final RestClient restClient = RestClient.create("https://oauth2.googleapis.com");

    public GoogleTokenInfoVerifier(@Value("${app.auth.google-client-id}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public GoogleUser verify(String idToken) {
        TokenInfo info;
        try {
            info = restClient.get().uri("/tokeninfo?id_token={t}", idToken).retrieve().body(TokenInfo.class);
        } catch (RestClientException e) {
            throw invalid();
        }
        if (info == null || !clientId.equals(info.aud) || !ISSUERS.contains(info.iss)
                || info.sub == null || info.email == null || !"true".equals(info.emailVerified)) {
            throw invalid();
        }
        return new GoogleUser(info.sub, info.email, info.name);
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "구글 로그인 확인에 실패했습니다.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenInfo {
        public String aud;
        public String iss;
        public String sub;
        public String email;
        public String name;
        @JsonProperty("email_verified")
        public String emailVerified;
    }
}
