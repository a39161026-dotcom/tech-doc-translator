package com.techdoc.translator.term;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=build/test-uploads",
        "spring.datasource.url=jdbc:h2:mem:term-api;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class TermApiTest {

    @Autowired
    MockMvc mockMvc;

    private String json(String source, String target) {
        return "{\"sourceTerm\":\"" + source + "\",\"targetTerm\":\"" + target + "\",\"note\":\"\"}";
    }

    private long create(String source, String target) throws Exception {
        String body = mockMvc.perform(post("/api/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(source, target)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void 용어를_등록하고_목록에서_본다() throws Exception {
        mockMvc.perform(post("/api/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  bean  ", "빈")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceTerm").value("bean"))
                .andExpect(jsonPath("$.targetTerm").value("빈"));

        mockMvc.perform(get("/api/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sourceTerm=='bean')]").exists());
    }

    @Test
    void 대소문자만_다른_같은_용어는_409() throws Exception {
        create("Controller", "컨트롤러");

        mockMvc.perform(post("/api/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("controller", "제어기")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TERM_DUPLICATED"));
    }

    @Test
    void 용어를_수정한다() throws Exception {
        long id = create("repository", "저장소");

        mockMvc.perform(put("/api/terms/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("repository", "리포지토리")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetTerm").value("리포지토리"));
    }

    @Test
    void 다른_용어와_겹치게_수정하면_409() throws Exception {
        create("entity", "엔티티");
        long id = create("transaction", "트랜잭션");

        mockMvc.perform(put("/api/terms/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Entity", "개체")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TERM_DUPLICATED"));
    }

    @Test
    void 용어를_삭제하면_없어진다() throws Exception {
        long id = create("annotation", "애너테이션");

        mockMvc.perform(delete("/api/terms/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/terms/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TERM_NOT_FOUND"));
    }

    @Test
    void 원어가_비어_있으면_400() throws Exception {
        mockMvc.perform(post("/api/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  ", "빈칸")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
