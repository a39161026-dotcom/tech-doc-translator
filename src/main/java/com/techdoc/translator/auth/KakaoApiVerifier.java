package com.techdoc.translator.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.techdoc.translator.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 카카오 API로 액세스 토큰을 확인한다. 먼저 토큰이 우리 앱(app_id)에서 발급된 것인지 확인하고,
 * 그다음 회원번호와 닉네임을 가져온다. 다른 앱의 토큰으로 로그인하는 것을 막기 위한 확인이다.
 */
@Component
public class KakaoApiVerifier implements KakaoTokenVerifier {

    private final long appId;
    private final RestClient restClient = RestClient.create("https://kapi.kakao.com");

    public KakaoApiVerifier(@Value("${app.auth.kakao-app-id}") long appId) {
        this.appId = appId;
    }

    @Override
    public KakaoUser verify(String accessToken) {
        String bearer = "Bearer " + accessToken;
        try {
            TokenInfo tokenInfo = restClient.get().uri("/v1/user/access_token_info")
                    .header("Authorization", bearer).retrieve().body(TokenInfo.class);
            if (tokenInfo == null || tokenInfo.appId != appId) {
                throw invalid();
            }
            UserInfo user = restClient.get().uri("/v2/user/me")
                    .header("Authorization", bearer).retrieve().body(UserInfo.class);
            if (user == null || user.id == null) {
                throw invalid();
            }
            String nickname = null;
            if (user.properties != null) {
                nickname = user.properties.get("nickname");
            }
            return new KakaoUser(String.valueOf(user.id), nickname);
        } catch (RestClientException e) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_KAKAO_TOKEN", "카카오 로그인 확인에 실패했습니다.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenInfo {
        @JsonProperty("app_id")
        public long appId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UserInfo {
        public Long id;
        public Map<String, String> properties;
    }
}
