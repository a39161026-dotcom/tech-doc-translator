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
        "spring.datasource.url=jdbc:h2:mem:google-login;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class GoogleLoginTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    GoogleTokenVerifier verifier;

    private String body(String token) {
        return "{\"idToken\":\"" + token + "\"}";
    }

    @Test
    void 구글_토큰이_맞으면_가입_후_토큰을_받고_같은_계정은_같은_회원이다() throws Exception {
        when(verifier.verify("good")).thenReturn(new GoogleTokenVerifier.GoogleUser("sub-1", "g1@example.com", "구글이"));

        String json = mockMvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(body("good")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.member.email").value("g1@example.com"))
                .andExpect(jsonPath("$.member.nickname").value("구글이"))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(json, "$.token");
        Integer id = com.jayway.jsonpath.JsonPath.read(json, "$.member.id");

        mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(body("good")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(id));
    }

    @Test
    void 같은_이메일로_가입한_회원은_구글_계정과_연결된다() throws Exception {
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"same@example.com\",\"password\":\"password1\",\"nickname\":\"기존\"}"))
                .andExpect(status().isCreated());
        when(verifier.verify("same")).thenReturn(new GoogleTokenVerifier.GoogleUser("sub-2", "Same@Example.com", "구글"));

        mockMvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(body("same")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.nickname").value("기존"));
    }

    @Test
    void 구글_확인에_실패하면_401() throws Exception {
        when(verifier.verify("bad")).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "실패"));

        mockMvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(body("bad")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_TOKEN"));
    }

    @Test
    void 토큰이_비어_있으면_400() throws Exception {
        mockMvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isBadRequest());
    }
}
