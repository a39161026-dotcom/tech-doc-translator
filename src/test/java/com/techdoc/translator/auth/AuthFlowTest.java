package com.techdoc.translator.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.auth.enabled=true",
        "app.upload-dir=build/test-uploads",
        "app.translation.async=false",
        "spring.datasource.url=jdbc:h2:mem:auth-flow;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class AuthFlowTest {

    @Autowired
    MockMvc mockMvc;

    private void signup(String email, String password) throws Exception {
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"nickname\":\"닉\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private long upload(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "d.txt", "text/plain", "One.".getBytes());
        String body = mockMvc.perform(multipart("/api/documents").file(file).header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void 토큰_없이_요청하면_401() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }

    @Test
    void 올바르지_않은_토큰이면_401() throws Exception {
        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void 회원가입_로그인_후_토큰으로_API를_쓸_수_있다() throws Exception {
        signup("flow@test.com", "password123");
        String token = login("flow@test.com", "password123");

        mockMvc.perform(get("/api/documents").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/members/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@test.com"));
    }

    @Test
    void 이메일이_중복되면_409_비밀번호가_짧으면_400() throws Exception {
        signup("dup@test.com", "password123");

        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"DUP@test.com\",\"password\":\"password123\",\"nickname\":\"닉\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"short@test.com\",\"password\":\"123\",\"nickname\":\"닉\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 비밀번호가_틀리면_401() throws Exception {
        signup("wrong@test.com", "password123");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@test.com\",\"password\":\"password999\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 남의_문서는_404로_보이고_목록에도_없다() throws Exception {
        signup("owner@test.com", "password123");
        signup("other@test.com", "password123");
        String owner = login("owner@test.com", "password123");
        String other = login("other@test.com", "password123");
        long id = upload(owner);

        mockMvc.perform(get("/api/documents/" + id).header("Authorization", owner)).andExpect(status().isOk());
        mockMvc.perform(get("/api/documents/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/documents/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/documents").header("Authorization", other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void CORS_사전_요청은_토큰_없이_통과한다() throws Exception {
        mockMvc.perform(options("/api/documents")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk());
    }
}
