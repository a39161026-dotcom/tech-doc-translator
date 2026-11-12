package com.techdoc.translator.translation;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=build/test-uploads",
        "app.translation.async=false",
        "spring.datasource.url=jdbc:h2:mem:translation-flow;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class TranslationFlowTest {

    @Autowired
    MockMvc mockMvc;

    private long upload(String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", content.getBytes());
        String body = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void 번역하면_모든_문단이_번역되고_문서가_DONE이_된다() throws Exception {
        long id = upload("Hello.\n\nWorld.");

        mockMvc.perform(post("/api/documents/" + id + "/translate"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.segmentCount").value(2))
                .andExpect(jsonPath("$.doneCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(0));

        mockMvc.perform(get("/api/documents/" + id + "/segments"))
                .andExpect(jsonPath("$[0].translatedText").value("[번역] Hello."))
                .andExpect(jsonPath("$[0].status").value("DONE"));
    }

    @Test
    void 실패한_문단이_있으면_FAILED이고_재시도하면_실패한_문단만_다시_번역한다() throws Exception {
        long id = upload("Good one.\n\nBad one FAIL_ONCE.\n\nAnother good one.");

        mockMvc.perform(post("/api/documents/" + id + "/translate"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.doneCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(1));

        mockMvc.perform(get("/api/documents/" + id + "/segments"))
                .andExpect(jsonPath("$[1].status").value("FAILED"))
                .andExpect(jsonPath("$[1].translatedText").doesNotExist());

        mockMvc.perform(post("/api/documents/" + id + "/retry"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.doneCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void 이미_번역한_문서를_다시_번역하면_409() throws Exception {
        long id = upload("Only one.");
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());

        mockMvc.perform(post("/api/documents/" + id + "/translate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    }

    @Test
    void 없는_문서는_404() throws Exception {
        mockMvc.perform(post("/api/documents/9999/translate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void 용어집에_등록한_용어가_번역에_적용된다() throws Exception {
        mockMvc.perform(post("/api/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceTerm\":\"dependency injection\",\"targetTerm\":\"의존성 주입\"}"))
                .andExpect(status().isCreated());
        long id = upload("Dependency injection is great.");

        mockMvc.perform(post("/api/documents/" + id + "/translate"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/documents/" + id + "/segments"))
                .andExpect(jsonPath("$[0].translatedText").value("[번역] 의존성 주입 is great."));
    }
}
