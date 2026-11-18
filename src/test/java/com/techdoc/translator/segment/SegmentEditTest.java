package com.techdoc.translator.segment;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.upload-dir=build/test-uploads",
        "app.translation.async=false",
        "spring.datasource.url=jdbc:h2:mem:segment-edit;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class SegmentEditTest {

    @Autowired
    MockMvc mockMvc;

    private long upload(String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", content.getBytes());
        String body = mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long segmentId(long documentId, int index) throws Exception {
        String body = mockMvc.perform(get("/api/documents/" + documentId + "/segments"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[" + index + "].id")).longValue();
    }

    private String json(String text) {
        return "{\"translatedText\":\"" + text + "\"}";
    }

    @Test
    void 실패한_문단을_직접_고치면_DONE이_되고_실패가_없으면_문서도_DONE이_된다() throws Exception {
        long id = upload("Good one.\n\nBad one FAIL.");
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());
        mockMvc.perform(get("/api/documents/" + id)).andExpect(jsonPath("$.status").value("FAILED"));

        mockMvc.perform(patch("/api/segments/" + segmentId(id, 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("직접 고친 번역")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translatedText").value("직접 고친 번역"))
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void 실패한_문단이_더_남아_있으면_문서는_FAILED로_남는다() throws Exception {
        long id = upload("Bad one FAIL.\n\nAnother bad FAIL.");
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());

        mockMvc.perform(patch("/api/segments/" + segmentId(id, 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("고침")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failedCount").value(1));
    }

    @Test
    void 번역이_끝난_문단도_고칠_수_있다() throws Exception {
        long id = upload("Hello.");
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());

        mockMvc.perform(patch("/api/segments/" + segmentId(id, 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("안녕하세요.")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents/" + id + "/segments"))
                .andExpect(jsonPath("$[0].translatedText").value("안녕하세요."));
        mockMvc.perform(get("/api/documents/" + id))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void 번역_전에는_고칠_수_없다() throws Exception {
        long id = upload("Not yet.");

        mockMvc.perform(patch("/api/segments/" + segmentId(id, 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("미리 고침")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_TRANSLATED"));
    }

    @Test
    void 빈_번역문은_400() throws Exception {
        long id = upload("Hello.");
        mockMvc.perform(post("/api/documents/" + id + "/translate")).andExpect(status().isAccepted());

        mockMvc.perform(patch("/api/segments/" + segmentId(id, 0))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 없는_문단은_404() throws Exception {
        mockMvc.perform(patch("/api/segments/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("없음")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEGMENT_NOT_FOUND"));
    }
}
