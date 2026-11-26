package com.techdoc.translator.auth;

import com.techdoc.translator.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.auth.enabled=true",
        "app.upload-dir=build/test-uploads",
        "spring.datasource.url=jdbc:h2:mem:kakao-login;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class KakaoLoginTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KakaoTokenVerifier verifier;

    private String body(String token) {
        return "{\"accessToken\":\"" + token + "\"}";
    }

    @Test
    void 카카오_토큰이_맞으면_가입_후_토큰을_받고_같은_계정은_같은_회원이다() throws Exception {
        when(verifier.verify("good")).thenReturn(new KakaoTokenVerifier.KakaoUser("12345", "카카오닉"));

        String json = mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON).content(body("good")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.member.email").value("kakao-12345@social.invalid"))
                .andExpect(jsonPath("$.member.nickname").value("카카오닉"))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(json, "$.token");
        Integer id = com.jayway.jsonpath.JsonPath.read(json, "$.member.id");

        mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON).content(body("good")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(id));
    }

    @Test
    void 닉네임이_없으면_기본_닉네임을_쓴다() throws Exception {
        when(verifier.verify("nonick")).thenReturn(new KakaoTokenVerifier.KakaoUser("777", null));

        mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON).content(body("nonick")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.nickname").value("사용자"));
    }

    @Test
    void 카카오_확인에_실패하면_401() throws Exception {
        when(verifier.verify("bad")).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_KAKAO_TOKEN", "실패"));

        mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON).content(body("bad")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_KAKAO_TOKEN"));
    }

    @Test
    void 토큰이_비어_있으면_400() throws Exception {
        mockMvc.perform(post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isBadRequest());
    }
}
